package net.researchgate.restdsl.queries;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.util.ThreadLocalDateFormat;
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a query to the storage
 * <p>
 * K - primary key type of the entity
 */

public final class ServiceQuery<K> {
    private static final int MAX_LIMIT = 10000;

    private Integer limit;
    private Integer originalLimit;
    private int offset = 0;
    private boolean countTotalItems = true;
    private String order;
    private Set<String> fields;
    private Collection<K> ids;
    private Multimap<String, Object> criteria;
    private boolean indexValidation = true;
    private String groupBy;
    private Boolean countOnly = false;

    // field that ensures that all subelements of these fields must match in "sync"
    // it's like $elemMatch in Mongo https://docs.mongodb.com/v3.2/reference/operator/query/elemMatch/
    private Set<String> syncMatch;

    // calculated fields
    private String queryShape;

    public boolean isCountTotalItems() {
        return countTotalItems;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getOrder() {
        return order;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public Boolean getCountOnly() {
        return countOnly;
    }

    public Set<String> getFields() {
        return fields;
    }

    public Multimap<String, Object> getCriteria() {
        return criteria;
    }

    public boolean isIndexValidation() {
        return indexValidation;
    }

    public Set<String> getSyncMatch() {
        return syncMatch;
    }

    public String getQueryShape() {
        return queryShape;
    }

    public static <K> ServiceQuery<K> all() {
        return new ServiceQueryBuilder<K>().build();
    }

    public static <K> ServiceQuery<K> byId(K id) {
        return new ServiceQueryBuilder<K>().ids(Collections.singletonList(id)).build();
    }

    public static <K> ServiceQuery<K> byIds(Iterable<K> ids) {
        return new ServiceQueryBuilder<K>().ids(ids).build();
    }


    @SuppressWarnings("unchecked")
    public static <K> ServiceQuery<K> byCriteria(String key, Object value) throws RestDslException {
        return new ServiceQueryBuilder<K>().withCriteria(key, Collections.singletonList(value)).build();
    }

    public Collection<K> getIdList() {
        return ids;
    }

    protected ServiceQuery() {
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ServiceQuery<?> && toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return toUrlPart();
    }


    public static <K> ServiceQueryBuilder<K> builder() {
        return new ServiceQueryBuilder<>();
    }

    public static class ServiceQueryBuilder<K> {
        // on purpose in the buider, so that the query is already formed when was built
        private ServiceQueryParams serviceQueryParams = ServiceQueryParams.ALL_QUERY_PARAMS;

        private ServiceQueryBuilder() {
        }

        private ServiceQuery<K> query = new ServiceQuery<>();

        public ServiceQueryBuilder<K> offset(Integer offset) throws RestDslException {
            if (offset != null) {
                if (offset < 0) {
                    throw new RestDslException("Offset cannot be less than 0");
                }
                query.offset = offset;
            }
            return this;
        }

        public ServiceQueryBuilder<K> limit(Integer limit) throws RestDslException {
            if (limit != null) {
                if (limit < 0) {
                    throw new RestDslException("Limit cannot be negative", RestDslException.Type.QUERY_ERROR);
                }
                query.limit = limit;
            }
            query.originalLimit = query.limit;
            return this;
        }

        public ServiceQueryBuilder<K> fields(Collection<String> fields) {
            if (fields != null) {
                query.fields = Collections.unmodifiableSet(Sets.newHashSet(fields));
            }
            return this;
        }

        public ServiceQueryBuilder<K> syncMatch(Collection<String> syncMatch) {
            if (syncMatch != null) {
                query.syncMatch = Collections.unmodifiableSet(Sets.newHashSet(syncMatch));
            }
            return this;
        }

        public ServiceQueryBuilder<K> indexValidation(Boolean indexValidation) {
            if (indexValidation != null) {
                query.indexValidation = indexValidation;
            }
            return this;
        }


        public ServiceQueryBuilder<K> order(String order) {
            query.order = order;
            return this;
        }

        public ServiceQueryBuilder<K> groupBy(String groupBy) {
            query.groupBy = groupBy;
            return this;
        }

        public ServiceQueryBuilder<K> id(K id) {
            query.ids = Collections.singletonList(id);
            return this;
        }

        public ServiceQueryBuilder<K> ids(Iterable<K> ids) {
            if (ids instanceof Collection) {
                query.ids = (Collection<K>) ids;
            } else {
                query.ids = Lists.newArrayList(ids);
            }
            return this;
        }

        public ServiceQueryBuilder<K> countTotalItems(Boolean countTotalItems) {
            if (countTotalItems != null) {
                query.countTotalItems = countTotalItems;
            }
            return this;
        }

        public ServiceQueryBuilder<K> withCriterion(String key, Object value) throws RestDslException {
            return withCriteria(key, Collections.singletonList(value));
        }

        public ServiceQueryBuilder<K> withCriteria(String key, Collection<?> value) throws RestDslException {
            if (value == null || value.isEmpty()) {
                throw new RestDslException("Criteria values for field '" + key + "' cannot be empty",
                        RestDslException.Type.QUERY_ERROR);
            }
            if (query.criteria == null) {
                query.criteria = HashMultimap.create();
            }

            query.criteria.putAll(key, value);
            return this;
        }

        // VZ: varargs are bad!
//        public ServiceQueryBuilder<K> withCriteria(String key, Object ... value) throws RestDslException {
//           return withCriteria(key, Arrays.asList(value));
//        }

        public ServiceQueryBuilder<K> withServiceQueryParams(ServiceQueryParams serviceQueryParams) {
            this.serviceQueryParams = serviceQueryParams;
            return this;
        }

        public ServiceQuery<K> build() {
            applyServiceQueryParams();
            query.calculateQueryShape();
            return query;
        }

        private void applyServiceQueryParams() {
            // CRITERIA
            Multimap<String, Object> defaultCriteria = serviceQueryParams.getDefaultCriteria();
            if (defaultCriteria != null && !defaultCriteria.isEmpty()) {
                Multimap<String, Object> combinedCriteria = query.criteria != null ? LinkedHashMultimap.create(query.criteria) : LinkedHashMultimap.create();
                // Merge defaultCriteria and user defined criteria
                for (String key : defaultCriteria.keys()) {
                    if (!combinedCriteria.containsKey(key)) {
                        combinedCriteria.putAll(key, defaultCriteria.get(key));
                    }
                }
                // ServiceQuery.ANY_VALUE is used as a placeholder for "$any" in criteria (see ServiceResource.parseRequest)
                combinedCriteria.keys().stream()
                        .filter(key -> combinedCriteria.get(key).size() == 1 &&
                                combinedCriteria.get(key).contains(ServiceQueryReservedValue.ANY)
                        ).forEach(combinedCriteria::removeAll);
                query.criteria = combinedCriteria;
            }

            // FIELDS
            if (query.fields == null) {
                query.fields = serviceQueryParams.getDefaultFields();
            }

            // LIMIT
            if (query.limit != null && query.limit == 0) {
                query.countOnly = true;
            }
            int curLimit = query.limit == null ? serviceQueryParams.getDefaultLimit() : query.limit;
            query.limit = curLimit > MAX_LIMIT || curLimit < 0 ? MAX_LIMIT : curLimit;

        }
    }

    //TODO : make sure that & won't affect toUrl or query shape
    public String toUrlPart() {
        StringBuilder sb = new StringBuilder();
        if (ids != null && !ids.isEmpty()) {
            sb.append(Joiner.on(",").join(ids));
        } else {
            sb.append("-;");
        }
        if (criteria != null) {
            for (String c : criteria.keySet()) {
                sb.append(c).append("=");
                sb.append(Joiner.on(',').join(criteria.get(c).stream()
                        .map(this::criteriaValToStr)
                        .collect(Collectors.toList()))).append(";");
            }
        }
        sb.append("?");
        sb.append("limit=").append(getLimit()).append("&");
        if (getOffset() != 0) {
            sb.append("offset=").append(getOffset()).append("&");
        }
        if (fields != null) {
            sb.append("fields=").append(Joiner.on(',').join(getFields())).append("&");
        }

        if (syncMatch != null) {
            sb.append("syncMatch=").append(Joiner.on(',').join(getSyncMatch())).append("&");
        }

        if (!indexValidation) {
            sb.append("indexValidation=false&");
        }

        if (order != null) {
            sb.append("order=").append(getOrder()).append("&");
        }

        return sb.toString();
    }

    private String criteriaValToStr(Object v) {
        if (v == null) {
            return ServiceQueryReservedValue.NULL.toString();
        }
        if (v instanceof ServiceQueryReservedValue) {
            return ((ServiceQueryReservedValue) v).getStrVal();
        }
        if (v instanceof Date) {
            return new ThreadLocalDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format((Date) v);
        }
        return v.toString();
    }

    private final static String QS_FIELD_SEP = "-";
    private final static String QS_KV_SEP = "-";
    private final static String QS_VAL_JOINER = "_";

    private void calculateQueryShape() {
        StringBuilder sb = new StringBuilder();
        if (!CollectionUtils.isEmpty(ids)) {
            sb.append("IDS").append(QS_FIELD_SEP);
        } else {
            sb.append(QS_FIELD_SEP);
        }

        if (criteria != null) {
            sb.append("CRITERIA").append(QS_KV_SEP);
            sb.append(Joiner.on(QS_VAL_JOINER).join(criteria.keySet()));
        }

        if (order != null) {
            sb.append(QS_FIELD_SEP).append("ORDER").append(QS_KV_SEP).append(order);
        }
        if (groupBy != null) {
            sb.append(QS_FIELD_SEP).append("GROUPBY").append(QS_KV_SEP).append(groupBy);
        }

        if (syncMatch != null) {
            sb.append(QS_FIELD_SEP).append("SYNCMATCH").append(QS_KV_SEP).append(syncMatch);
        }

        if (originalLimit != null) {
            sb.append(QS_FIELD_SEP).append("LIMIT");
        }

        queryShape = sb.toString();
    }

}
