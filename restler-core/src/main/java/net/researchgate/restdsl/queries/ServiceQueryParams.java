package net.researchgate.restdsl.queries;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Created by zholudev on 20/04/16.
 * Default params for service query
 */
public interface ServiceQueryParams {
    ServiceQueryParams ALL_QUERY_PARAMS =
            ServiceQueryParamsImpl.builder().defaultLimit(Integer.MAX_VALUE).defaultFields(Sets.newHashSet("*")).build();

    ServiceQueryParams DEFAULT_QUERY_PARAMS =
            ServiceQueryParamsImpl.builder().defaultLimit(100).defaultFields(Sets.newHashSet("*")).build();

    int getDefaultLimit();
    Set<String> getDefaultFields();
    Multimap<String, Object> getDefaultCriteria();
}
