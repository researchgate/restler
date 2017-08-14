package net.researchgate.restdsl.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.researchgate.restdsl.domain.EntityInfo;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.PatchContext;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.queries.ServiceQueryParams;
import net.researchgate.restdsl.types.TypeInfoUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Utilities for dealing with requests
 */
public class RequestUtil {

    public static Integer getInt(String key, UriInfo uriInfo) throws RestDslException {
        MultivaluedMap<String, String> map = uriInfo.getQueryParameters();
        if (!map.containsKey(key)) {
            return null;
        }
        try {
            return Integer.valueOf(map.getFirst(key), 10);
        } catch (NumberFormatException e) {
            throw new RestDslException("Cannot parse integer from '" + key + "'", RestDslException.Type.PARAMS_ERROR);
        }
    }

    public static Boolean getBoolean(String key, UriInfo uriInfo) {
        MultivaluedMap<String, String> map = uriInfo.getQueryParameters();
        if (!map.containsKey(key)) {
            return null;
        }
        return Boolean.valueOf(map.getFirst(key));
    }

    public static String getString(String key, UriInfo uriInfo) {
        MultivaluedMap<String, String> map = uriInfo.getQueryParameters();
        if (!map.containsKey(key)) {
            return null;
        }
        return map.getFirst(key);
    }


    public static List<String> getToList(String key, UriInfo uriInfo) {
        MultivaluedMap<String, String> map = uriInfo.getQueryParameters();
        if (!map.containsKey(key)) {
            return null;
        }
        return Splitter.on(',').splitToList(map.getFirst(key));
    }

    public static Set<String> getToSet(String key, UriInfo uriInfo) {
        MultivaluedMap<String, String> map = uriInfo.getQueryParameters();
        if (!map.containsKey(key)) {
            return null;
        }
        return Sets.newHashSet(Splitter.on(',').split(map.getFirst(key)));
    }

    public static PatchContext getPatchContext(UriInfo uriInfo) throws RestDslException {
        Set<String> unsetFields = getToSet("unsetFields", uriInfo);
        return unsetFields == null ? PatchContext.DEFAULT_CONTEXT : PatchContext.builder().unsetFields(unsetFields).build();

    }

    public static <K, V> ServiceQuery<K> parseRequest(Class<V> entityClazz, Class<K> idClazz, PathSegment segment, UriInfo uriInfo, ServiceQueryParams serviceQueryParams) throws RestDslException {
        ServiceQuery.ServiceQueryBuilder<K> builder = ServiceQuery.builder();

        builder.offset(getInt("offset", uriInfo));
        builder.limit(getInt("limit", uriInfo));
        builder.fields(getToList("fields", uriInfo));
        builder.order(uriInfo.getQueryParameters().getFirst("order"));
        builder.indexValidation(getBoolean("indexValidation", uriInfo));
        builder.countTotalItems(getBoolean("countTotalItems", uriInfo));
        builder.groupBy(getString("groupBy", uriInfo));
        builder.withServiceQueryParams(serviceQueryParams);
        builder.syncMatch(getToList("syncMatch", uriInfo));
        builder.slaveOk(getBoolean("slaveOk", uriInfo));

        MultivaluedMap<String, String> matrixParams = segment.getMatrixParameters();
        if (!matrixParams.isEmpty()) {
            for (String fieldNameWithCriteria : matrixParams.keySet()) {
                Collection<String> values = matrixParams.get(fieldNameWithCriteria);
                // splitting comma-separated values
                Collection<String> splitValues = new ArrayList<>();
                for (String v : values) {
                    splitValues.addAll(Splitter.on(',').omitEmptyStrings().splitToList(v));
                }
                ServiceQueryUtil.ParsedQueryField parsedQueryField = ServiceQueryUtil.parseQueryField(fieldNameWithCriteria);
                String fieldNameWithoutConditions = parsedQueryField.getFieldName();
                Pair<Class<?>, Class<?>> pairOfFieldClazzAndParentClazz =
                        TypeInfoUtil.getFieldExpressionClazz(entityClazz, fieldNameWithoutConditions);

                Class<?> fieldClazz = pairOfFieldClazzAndParentClazz.getLeft();
                Class<?> parentClazz = pairOfFieldClazzAndParentClazz.getRight();

                List<Object> criteriaList = Lists.newArrayList(Iterables.transform(splitValues,
                        input -> TypeInfoUtil.getValue(input, fieldNameWithoutConditions, fieldClazz, parentClazz)));

                builder.withCriteria(parsedQueryField.getFullCriteria(), criteriaList);
            }
        }
        if (!StringUtils.isEmpty(segment.getPath()) && !segment.getPath().startsWith("-")) {
            builder.ids(Lists.transform(Splitter.on(',').splitToList(segment.getPath()),
                    input -> TypeInfoUtil.getValue(input, EntityInfo.get(entityClazz).getIdFieldName(), idClazz, entityClazz)));
        }

        return builder.build();
    }

}
