package net.researchgate.restdsl.dao;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.query.filters.Filter;
import net.researchgate.restdsl.domain.EntityIndexInfo;
import net.researchgate.restdsl.domain.EntityInfo;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.metrics.MetricName;
import net.researchgate.restdsl.metrics.MetricSink;
import net.researchgate.restdsl.metrics.MetricSinkTimingWrapper;
import net.researchgate.restdsl.metrics.NoOpMetricSink;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.queries.ServiceQueryInfo;
import net.researchgate.restdsl.queries.ServiceQueryReservedValue;
import net.researchgate.restdsl.results.EntityList;
import net.researchgate.restdsl.results.EntityMultimap;
import net.researchgate.restdsl.results.EntityResult;
import net.researchgate.restdsl.util.ServiceQueryUtil;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.morphia.query.filters.Filters.elemMatch;
import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.exists;
import static dev.morphia.query.filters.Filters.gt;
import static dev.morphia.query.filters.Filters.gte;
import static dev.morphia.query.filters.Filters.in;
import static dev.morphia.query.filters.Filters.lt;
import static dev.morphia.query.filters.Filters.lte;
import static dev.morphia.query.filters.Filters.nin;

/**
 * This Dao implements common access to the underlying mongo collection.
 * Use this dao if you want to make sure that your data is only written to in a controlled way.
 * <p>Supported operations</p>
 * <ul>
 *     <li>get by restler dsl</li>
 *     <li>delete by id</li>
 *</ul>
 * <p>If you want to simply expose CRUD via REST, use a {@link MongoServiceDao}.</p>
 *
 * @param <V> Type of the entity
 * @param <K> Type of the entity's id field
 */
public class MongoBaseServiceDao<V, K> implements BaseServiceDao<V, K>{
    public static final String MONGO_SERVICE_QUERY_METRIC = "mongo_service_query";
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoBaseServiceDao.class);

    protected final String collectionName;
    protected final Class<V> entityClazz;
    protected final EntityInfo<V> entityInfo;
    protected final EntityIndexInfo<V> entityIndexInfo;
    protected final MetricSink metricSink;

    // group by operations may require a lot of requests to the database. We should have to explicitly enable it
    protected boolean allowGroupBy = false;

    protected final Datastore datastore;
    private final EntityFieldMapper entityMapper;

    @SuppressWarnings("unused")
    public MongoBaseServiceDao(Datastore datastore, Class<V> entityClazz) {
        this(datastore, entityClazz, NoOpMetricSink.INSTANCE);
    }

    /**
     * Creates a new DAO.
     *
     * @param datastore Morphia datastore to query and update
     * @param entityClazz Class of objects to bind MongoDB documents to
     * @param statsReporter Instrumentation to collect query durations
     *
     * @deprecated use {@link #MongoBaseServiceDao(Datastore, Class, MetricSink)}
     */
    @Deprecated
    @SuppressWarnings("removal")
    public MongoBaseServiceDao(Datastore datastore, Class<V> entityClazz, net.researchgate.restdsl.metrics.StatsReporter statsReporter) {
        this(datastore, entityClazz, new net.researchgate.restdsl.metrics.MetricSinkStatsReporterAdaptor(statsReporter));
    }

    public MongoBaseServiceDao(Datastore datastore, Class<V> entityClazz, MetricSink metricSink) {
        this.datastore = datastore;
        this.collectionName = datastore.getMapper().getEntityModel(entityClazz).getCollectionName();
        this.entityClazz = entityClazz;
        this.entityMapper = new MongoEntityFieldMapper(datastore);
        this.entityInfo = EntityInfo.get(entityMapper, entityClazz);
        this.entityIndexInfo = new EntityIndexInfo<>(datastore, entityClazz, datastore.getCollection(entityClazz).listIndexes());
        this.metricSink = metricSink;
        validateMorphiaAnnotations(entityClazz, new HashSet<>());
    }

    @Override
    public EntityFieldMapper getEntityMapper() {
        return entityMapper;
    }

    /**
     * Explicitly allow groupBy in get queries. If not, we will throw a RestDsl exception when the client queries with
     * a groupBy.
     */
    protected void setAllowGroupBy() {
        this.allowGroupBy = true;
    }

    public FindOptions toFindOptions(ServiceQuery<K>  serviceQuery) {

        FindOptions findOptions = new FindOptions();

        if (serviceQuery.getFields() != null) {
            Set<String> excludedFields = Sets.newHashSet();
            Set<String> includedFields = Sets.newHashSet();
            boolean all = false;
            for (String f : serviceQuery.getFields()) {
                if (f.equals("*")) {
                    all = true;
                } else {
                    if (f.startsWith("-")) {
                        excludedFields.add(f.substring(1));
                    } else {
                        includedFields.add(f);
                    }
                }
            }

            if (!excludedFields.isEmpty() && !includedFields.isEmpty()) {
                throw new RestDslException("Query cannot have both included and excluded fields", RestDslException.Type.QUERY_ERROR);
            }

            if (!all) {
                if (!includedFields.isEmpty()) {
                    findOptions.projection().include(includedFields.toArray(new String[0]));
                } else {
                    // only excluded fields were provided
                    findOptions.projection().include(excludedFields.toArray(new String[0]));
                }
            } else {
                // provided * but also excluded fields
                if (!excludedFields.isEmpty()) {
                    findOptions.projection().include(excludedFields.toArray(new String[0]));
                }
            }
        }

        findOptions.skip(serviceQuery.getOffset());
        findOptions.limit(serviceQuery.getLimit());

        if (serviceQuery.getOrder() != null) {
            findOptions.sort(parseSortString(serviceQuery.getOrder()));
        }
        return findOptions;
    }

    private static Sort[] parseSortString(String sortString) {
        if (sortString == null || sortString.isBlank()) {
            return new Sort[0];
        }

        String[] tokens = sortString.split(",");
        List<Sort> sorts = new ArrayList<>();

        for (String token : tokens) {
            token = token.trim();
            if (token.isEmpty())
                continue;

            if (token.startsWith("-")) {
                sorts.add(Sort.descending(token.substring(1)));
            } else {
                sorts.add(Sort.ascending(token));
            }
        }

        return sorts.toArray(new Sort[0]);
    }

    public EntityResult<V> get(ServiceQuery<K> serviceQuery) throws RestDslException {
        Query<V> morphiaQuery = convertToMorphiaQuery(serviceQuery);
        FindOptions findOptions = toFindOptions(serviceQuery);

        try (MetricSinkTimingWrapper ignored = getQueryShapeWrapper(serviceQuery)) {
            String groupBy = serviceQuery.getGroupBy();
            if (groupBy == null) {
                List<V> results = Collections.emptyList();
                if (!serviceQuery.getCountOnly()) {
                    LOGGER.debug("Executing query {}", morphiaQuery);
                    try (MorphiaCursor<V> iterator = morphiaQuery.iterator(findOptions)) {
                        results = iterator.toList();
                    }
                }
                return new EntityResult<>(results, getTotalItemsCnt(morphiaQuery, serviceQuery, results));
            } else {
                if (!allowGroupBy) {
                    throw new RestDslException("GroupBy is not allowed by this dao, but request contains groupBy '" + groupBy + "'. GroupBy can be enabled in the Service", RestDslException.Type.QUERY_ERROR);
                }
                // warning: in-place criteria editing
                Collection<Object> criteriaForGrouping = Lists.newArrayList(serviceQuery.getCriteria().get(groupBy));

                Map<Object, EntityList<V>> groupedResult = new HashMap<>();
                for (Object k : criteriaForGrouping) {
                    serviceQuery.getCriteria().removeAll(groupBy);
                    serviceQuery.getCriteria().put(groupBy, k);
                    Query<V> q = convertToMorphiaQuery(serviceQuery);
                    List<V> resultPerKey = Collections.emptyList();
                    if (!serviceQuery.getCountOnly()) {
                        LOGGER.debug("Executing query {}", q);
                        try (MorphiaCursor<V> iterator = q.iterator(findOptions)) {
                            resultPerKey = iterator.toList();
                        }
                    }

                    EntityList<V> entityList = new EntityList<>(resultPerKey, getTotalItemsCnt(q, serviceQuery, resultPerKey));
                    groupedResult.put(k, entityList);
                }
                return new EntityResult<>(new EntityMultimap<>(groupedResult, serviceQuery.isCountTotalItems() ? morphiaQuery.count() : null));
            }
        }
    }

    public V getOne(ServiceQuery<K> serviceQuery) throws RestDslException {
        return convertToMorphiaQuery(serviceQuery).first(toFindOptions(serviceQuery));
    }

    public long count(ServiceQuery<K> serviceQuery) throws RestDslException {
        return convertToMorphiaQuery(serviceQuery).count();
    }

    public int delete(K id) {
        return Math.toIntExact(datastore.find(entityClazz)
                .filter(eq("_id", id))
                .delete(new DeleteOptions().multi(false))
                .getDeletedCount());
    }

    Query<V> convertToMorphiaQuery(ServiceQuery<K> serviceQuery) throws RestDslException {
        validateQuery(serviceQuery);

        Query<V> mongoQuery = datastore.find(entityClazz);

        Collection<K> ids = serviceQuery.getIdList();
        if (ids != null) {

            if (ids.size() == 1) {
                mongoQuery.filter(eq(entityInfo.getIdFieldName(), ids.iterator().next()));
            } else {
                mongoQuery.filter(in(entityInfo.getIdFieldName(), ids));
            }
        }

        if (serviceQuery.getCriteria() != null) {
            Multimap<String, String> syncMatchToCriteriaKeys = HashMultimap.create();
            Set<String> syncMatch = serviceQuery.getSyncMatch() == null ? Collections.emptySet() : serviceQuery.getSyncMatch();

            for (String k : serviceQuery.getCriteria().keySet()) {
                boolean forSyncMatch = false;
                for (String sm : syncMatch) {
                    if (k.startsWith(sm + ".")) {
                        syncMatchToCriteriaKeys.put(sm, k);
                        forSyncMatch = true;
                    }
                }

                if (forSyncMatch) {
                    continue;
                }

                Collection<Object> objs = serviceQuery.getCriteria().get(k);
                mongoQuery.filter(prepareFilters(k, objs));
            }

            if (!syncMatchToCriteriaKeys.isEmpty()) {
                for (String k : syncMatchToCriteriaKeys.keySet()) {
                    Collection<String> criteriaKeys = syncMatchToCriteriaKeys.get(k);
                    List<Filter> elemMatchFilters = Lists.newArrayList();
                    for (String criteriaKey : criteriaKeys) {
                        elemMatchFilters.addAll(List.of(prepareFilters(criteriaKey.substring(k.length() + 1), serviceQuery.getCriteria().get(criteriaKey))));
                    }

                    mongoQuery.filter(elemMatch(k, elemMatchFilters.toArray(new Filter[0])));
                }
            }
        }

        return mongoQuery;
    }

    private Filter[] prepareFilters(String field, Collection<Object> criteria) {
        List<Filter> filters = Lists.newArrayList();
        if (criteria.size() == 1) {
            // range query
            Object val = criteria.iterator().next();
            // TODO: rather operatte on ParsedField and have a method to determine whether it's some operation
            if (field.contains(">") || field.contains("<")) {
                final String[] parts = field.trim().split(" ");
                if (parts.length < 2) {
                    throw new IllegalArgumentException("'" + field + "' is not a valid filter condition");
                }
                String operator = parts[1];
                if ("<".equals(operator)) {
                    filters.add(lt(parts[0], val));
                } else if ("<=".equals(operator)) {
                    filters.add(lte(parts[0], val));
                } else if (">=".equals(operator)) {
                    filters.add(gte(parts[0], val));
                } else if (">".equals(operator)) {
                    filters.add(gt(parts[0], val));
                }
            } else if (val instanceof ServiceQueryReservedValue) {
                if (val == ServiceQueryReservedValue.EXISTS) {
                    filters.add(exists(field));
                } else if (val == ServiceQueryReservedValue.NULL) {
                    filters.add(eq(field, null));
                } else if (val == ServiceQueryReservedValue.ANY) {
                    // no op
                } else {
                    throw new RestDslException("Unhandled reserved value: " + val, RestDslException.Type.GENERAL_ERROR);
                }
            } else {
                filters.add(eq(field, val));
            }
        } else {
            // TODO: deal nicely with other operations
            if (field.contains("<>")) {
                filters.add(nin(ServiceQueryUtil.parseQueryField(field).getFieldName(), criteria.toArray(new Object[0])));
            } else {
                // defensive copy here to avoid issues with groupBy
                filters.add(in(field, new ArrayList<>(criteria)));
            }
        }
        return filters.toArray(new Filter[0]);
    }

    // optimization. If the returned set is smaller than limit, that means we can calculate size without countAll()
    private Long getTotalItemsCnt(Query<V> q, ServiceQuery<?> serviceQuery, List<V> results) {
        if (!serviceQuery.isCountTotalItems()) {
            return null;
        }

        if (results.size() > serviceQuery.getLimit()) {
            throw new RestDslException("Implementation error: results size must be not greater than limit, was " +
                    results.size() + " but limit was: " + serviceQuery.getLimit());
        }

        if (serviceQuery.getCountOnly()) {
            return q.count();
        }

        // if getLimit == 0 then we need to count anyway
        if (results.isEmpty() && serviceQuery.getOffset() == 0 && serviceQuery.getLimit() > 0) {
            return 0L;
        }

        // if size is equal 0 it could be that offset is too big, or we just have 0 elements in total - must count
        if (!results.isEmpty() && results.size() < serviceQuery.getLimit()) {
            return (long) (serviceQuery.getOffset() + results.size());
        } else {
            return q.count();
        }
    }

    public void validateQuery(ServiceQuery<K> serviceQuery) throws RestDslException {
        if (serviceQuery.isIndexValidation()) {
            boolean safeQuery = isSafeQuery(serviceQuery);
            if (!safeQuery) {
                throw new RestDslException("Query criterion for fields " + serviceQuery.getCriteria().keySet() +
                        " don't match declared indexes  [(" + Joiner.on("), (").join(entityIndexInfo.getIndexesMap()) +
                        ")] for class " + entityClazz.getName() +
                        "; use '?indexValidation=false' query parameter to temporarily disable it for debugging purposes.",
                        RestDslException.Type.QUERY_ERROR);
            }
        }

        if (serviceQuery.getLimit() < 0) {
            throw new RestDslException("Query limit must be positive", RestDslException.Type.QUERY_ERROR);
        }

        // for primary keys it could also work, but it does not make sense to group on them since they are unique
        if (serviceQuery.getGroupBy() != null &&
                (serviceQuery.getCriteria() == null || !serviceQuery.getCriteria().containsKey(serviceQuery.getGroupBy()))) {
            throw new RestDslException("When provided, groupBy parameter should be contained in query criteria",
                    RestDslException.Type.QUERY_ERROR);
        }

    }

    public ServiceQueryInfo<K> getServiceQueryInfo(ServiceQuery<K> serviceQuery) {
        return new ServiceQueryInfo<>(serviceQuery, isSafeQuery(serviceQuery));
    }

    private boolean isSafeQuery(ServiceQuery<K> serviceQuery) {
        boolean queryIsSafe = true;
        // criteria is not empty and primary keys are not specified, then we need to check whether index is used
        boolean shouldCheckForIndex = serviceQuery.getCriteria() != null && !serviceQuery.getCriteria().isEmpty()
                && CollectionUtils.isEmpty(serviceQuery.getIdList());

        if (shouldCheckForIndex) {
            queryIsSafe = false;
            for (String field : serviceQuery.getCriteria().keySet()) {
                if (entityIndexInfo.getIndexPrefixMap().contains(ServiceQueryUtil.parseQueryField(field).getFieldName())) {
                    queryIsSafe = true;
                    break;
                }
            }
        }
        return queryIsSafe;
    }

    // PRIVATE
    private MetricSinkTimingWrapper getQueryShapeWrapper(ServiceQuery<K> serviceQuery) {
        MetricName name = new MetricName(
                MONGO_SERVICE_QUERY_METRIC,
                Map.of(
                        "collectionName", collectionName,
                        "queryShape", serviceQuery.getQueryShape()
                )
        );
        return MetricSinkTimingWrapper.of(metricSink, name);
    }

    // TODO Remove this after the morphia migration.
    // This method checks, that there are no old 'org.mongodb.morphia' annotations present on the morphia managed entities.
    private void validateMorphiaAnnotations(Class<?> klass, Set<Class<?>> seen) {
        if (!seen.add(klass)) { // break circular deps
            return;
        }

        for (Annotation cAnno : klass.getDeclaredAnnotations()) {
            if (isBadAnnotation(cAnno)) {
                throw new IllegalStateException("Incompatible morphia annotation found at " + klass.getName()
                        + "\n annotation: " + cAnno.annotationType()
                        + "\n Make sure all you domain objects are using only 'dev.morphia' annotations");
            }

        }
        for (Field field : klass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            for (Annotation anno : field.getDeclaredAnnotations()) {
                if (isBadAnnotation(anno)) {
                    throw new IllegalStateException("Incompatible morphia annotation found at " + field
                            + "\n annotation: " + anno.annotationType()
                            + "\n Make sure all you domain objects are using only 'dev.morphia' annotations");
                }
            }
            validateMorphiaAnnotations(field.getType(), seen);
        }

    }

    private boolean isBadAnnotation(Annotation cAnno) {
        Class<? extends Annotation> type = cAnno.annotationType();
        if (type == null) {
            return false;
        }
        return type.getTypeName().startsWith("org.mongodb.morphia");
    }
}
