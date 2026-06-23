package net.researchgate.restdsl.queries;

import com.google.common.collect.Multimap;

import java.util.HashSet;
import java.util.Set;

/**
 * Default params for service query
 */
public interface ServiceQueryParams {
    ServiceQueryParams ALL_QUERY_PARAMS =
            ServiceQueryParamsImpl.builder().defaultLimit(Integer.MAX_VALUE).defaultFields(new HashSet<>(Set.of("*"))).build();

    ServiceQueryParams DEFAULT_QUERY_PARAMS =
            ServiceQueryParamsImpl.builder().defaultLimit(100).defaultFields(new HashSet<>(Set.of("*"))).build();

    int getDefaultLimit();
    Set<String> getDefaultFields();
    Multimap<String, Object> getDefaultCriteria();
}
