package net.researchgate.restdsl.dao;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import net.researchgate.restdsl.domain.EntityIndexInfo;
import net.researchgate.restdsl.domain.EntityInfo;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.metrics.NoOpStatsReporter;
import net.researchgate.restdsl.metrics.StatsReporter;
import net.researchgate.restdsl.metrics.StatsTimingWrapper;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.queries.ServiceQueryInfo;
import net.researchgate.restdsl.queries.ServiceQueryReservedValue;
import net.researchgate.restdsl.results.EntityList;
import net.researchgate.restdsl.results.EntityMultimap;
import net.researchgate.restdsl.results.EntityResult;
import net.researchgate.restdsl.types.TypeInfoUtil;
import net.researchgate.restdsl.util.ServiceQueryUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import dev.morphia.Datastore;
import dev.morphia.dao.BasicDAO;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This Dao implements common access to the underlying mongo collection.
 * Use this dao if you want to make sure that your data is only written to in a controlled way.
 *
 * Supported operations
 * - get by restler dsl
 * - delete by id
 *
 * If you want to simply expose CRUD via REST, use a {@link MongoServiceDao}.
 *
 * @param <V> Type of the entity
 * @param <K> Type of the entity's id field
 */
public class MongoBaseServiceDao<V, K> implements BaseServiceDao<V, K>{
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoBaseServiceDao.class);

    private static final String QUERY_KEY = "queries.shapes.%s.%%H";
    protected final String collectionName;
    protected final BasicDAO<V, K> morphiaDao;
    protected final Class<V> entityClazz;
    protected final EntityInfo<V> entityInfo;
    protected final EntityIndexInfo<V> entityIndexInfo;
    protected final StatsReporter statsReporter;

    // group by operations may require a lot of requests to the database. We should have to explicitly enable it
    protected boolean allowGroupBy = false;

    public MongoBaseServiceDao(Datastore datastore, Class<V> entityClazz) {
        this(datastore, entityClazz, NoOpStatsReporter.INSTANCE);
    }
    //TODO: provide implementations for StatsReporter in example service
    public MongoBaseServiceDao(Datastore datastore, Class<V> entityClazz, StatsReporter statsReporter) {
        this.morphiaDao = new BasicDAO<>(entityClazz, datastore);
        this.collectionName = morphiaDao.getCollection().getName();
        this.entityClazz = entityClazz;
        this.entityInfo = EntityInfo.get(entityClazz);
        this.entityIndexInfo = new EntityIndexInfo<>(entityClazz, morphiaDao.getCollection().getIndexInfo());
        this.statsReporter = statsReporter;
        validateMorphiaAnnotations(entityClazz, new HashSet<>());
    }

    /**
     * explicitly allow groupBy in get queries.
     *
     * If not, we will throw a RestDsl exception when the client queries with a groupBy.
     */
    protected void setAllowGroupBy() {
        this.allowGroupBy = true;
    }

    public EntityResult<V> get(ServiceQuery<K> serviceQuery) throws RestDslException {
        Query<V> morphiaQuery = convertToMorphiaQuery(serviceQuery);

        try (StatsTimingWrapper ignored = getQueryShapeWrapper(serviceQuery)) {
            String groupBy = serviceQuery.getGroupBy();
            if (groupBy == null) {
                List<V> results = Collections.emptyList();
                if (!serviceQuery.getCountOnly()) {
                    LOGGER.debug("Executing query {}", morphiaQuery);
                    results = morphiaDao.find(morphiaQuery).asList();
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
                        resultPerKey = morphiaDao.find(q).asList();
                    }

                    EntityList<V> entityList = new EntityList<>(resultPerKey, getTotalItemsCnt(q, serviceQuery, resultPerKey));
                    groupedResult.put(k, entityList);
                }
                return new EntityResult<>(new EntityMultimap<>(groupedResult, serviceQuery.isCountTotalItems() ? morphiaQuery.count() : null));
            }
        }
    }

    public V getOne(ServiceQuery<K> serviceQuery) throws RestDslException {
        return morphiaDao.findOne(convertToMorphiaQuery(serviceQuery));
    }

    public long count(ServiceQuery<K> serviceQuery) throws RestDslException {
        return convertToMorphiaQuery(serviceQuery).count();
    }

    public int delete(K id) {
        return morphiaDao.deleteById(id).getN();
    }

    protected UpdateOperations<V> createUpdateOperations() {
        return morphiaDao.createUpdateOperations();
    }

    protected Query<V> convertToMorphiaQuery(ServiceQuery<K> serviceQuery) throws RestDslException {
        return convertToMorphiaQuery(serviceQuery, true);
    }

    protected Query<V> convertToMorphiaQuery(ServiceQuery<K> serviceQuery, boolean getQuery) throws RestDslException {
        validateQuery(serviceQuery);

        Query<V> mongoQuery = morphiaDao.createQuery();

        Collection<K> ids = serviceQuery.getIdList();
        if (ids != null) {
            if (ids.size() == 1) {
                mongoQuery.field(entityInfo.getIdFieldName()).equal(ids.iterator().next());
            } else {
                mongoQuery.field(entityInfo.getIdFieldName()).hasAnyOf(ids);
            }
        }

        if (getQuery) {
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
                        includedFields.forEach(f -> mongoQuery.project(f, true));
                    } else {
                        // only excluded fields were provided
                        excludedFields.forEach(f -> mongoQuery.project(f, false));
                    }
                } else {
                    // provided * but also excluded fields
                    if (!excludedFields.isEmpty()) {
                        excludedFields.forEach(f -> mongoQuery.project(f, false));
                    }
                }
            }

            mongoQuery.offset(serviceQuery.getOffset());
            mongoQuery.limit(serviceQuery.getLimit());

            if (serviceQuery.getOrder() != null) {
                mongoQuery.order(serviceQuery.getOrder());
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
                enrichQuery(mongoQuery, k, objs);
            }

            if (!syncMatchToCriteriaKeys.isEmpty()) {
                for (String k : syncMatchToCriteriaKeys.keySet()) {
                    Collection<String> criteriaKeys = syncMatchToCriteriaKeys.get(k);

                    Pair<Class<?>, Class<?>> fieldExpressionClazz = TypeInfoUtil.getFieldExpressionClazz(entityClazz, k);
                    Query<?> subFieldQuery = morphiaDao.getDatastore().createQuery(fieldExpressionClazz.getLeft());
                    for (String criteriaKey : criteriaKeys) {
                        enrichQuery(subFieldQuery, criteriaKey.substring(k.length() + 1), serviceQuery.getCriteria().get(criteriaKey));
                    }

                    mongoQuery.field(k).elemMatch(subFieldQuery);
                }
            }
        }

        return mongoQuery;
    }

    private void enrichQuery(Query<?> mongoQuery, String field, Collection<Object> criteria) {
        if (criteria.size() == 1) {
            // range query
            Object val = criteria.iterator().next();
            // TODO: rather operatte on ParsedField and have a method to determine whether it's some operation
            if (field.contains(">") || field.contains("<")) {
                mongoQuery.filter(field, val);
            } else if (val instanceof ServiceQueryReservedValue) {
                adaptToReservedValue(mongoQuery, field, (ServiceQueryReservedValue) val);
            } else {
                mongoQuery.field(field).equal(val);
            }
        } else {
            // TODO: deal nicely with other operations
            if (field.contains("<>")) {
                mongoQuery.field(ServiceQueryUtil.parseQueryField(field).getFieldName()).notIn(criteria);
            } else {
                mongoQuery.field(field).in(criteria);
            }
        }
    }

    // optimization. If the returned set is smaller than limit, that means we can calculate size without countAll()
    private Long getTotalItemsCnt(Query<V> q, ServiceQuery<?> serviceQuery, List<V> results) {
        if (!serviceQuery.isCountTotalItems()) {
            return null;
        }

        if (results.size() > q.getLimit()) {
            throw new RestDslException("Implementation error: results size must be not greater than limit, was " +
                    results.size() + " but limit was: " + q.getLimit());
        }

        if (serviceQuery.getCountOnly()) {
            return q.count();
        }

        // if getLimit == 0 then we need to count anyway
        if (results.size() == 0 && q.getOffset() == 0 && q.getLimit() > 0) {
            return 0L;
        }

        // if size is equal 0 it could be that offset is too big, or we just have 0 elements in total - must count
        if (results.size() != 0 && results.size() < q.getLimit()) {
            return (long) (q.getOffset() + results.size());
        } else {
            return q.count();
        }
    }

    private void adaptToReservedValue(Query<?> mongoQuery, String k, ServiceQueryReservedValue val) {
        if (val == ServiceQueryReservedValue.EXISTS) {
            mongoQuery.criteria(k).exists();
        } else if (val == ServiceQueryReservedValue.NULL) {
            mongoQuery.field(k).equal(null);
        } else if (val == ServiceQueryReservedValue.ANY) {
            // no op
        } else {
            throw new RestDslException("Unhandled reserved value: " + val, RestDslException.Type.GENERAL_ERROR);
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

        // for primary keys it could also works, but it does not make sense to group on them since they are unique
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
    private StatsTimingWrapper getQueryShapeWrapper(ServiceQuery<K> serviceQuery) {
        return StatsTimingWrapper.of(statsReporter, String.format(QUERY_KEY, collectionName + "." + serviceQuery.getQueryShape()));
    }


    // TODO Remove this after the morphia migration.
    // This method checks, that there are no old 'org.mongodb.morphia' annotations present on the morphia managed entities.
    private void validateMorphiaAnnotations(Class<?> klass, Set<Class> seen) {
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
