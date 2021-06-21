package net.researchgate.restdsl.dao;

import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.queries.ServiceQueryInfo;
import net.researchgate.restdsl.results.EntityResult;

/**
 * Abstract representation of a reading DAO. Can be Solr dao, or PG dao in theory
 * @param <V> - value entity
 * @param <K> - primary key of the value entity
 */
public interface ServiceDao<V, K> {

    EntityResult<V> get(ServiceQuery<K> serviceQuery) throws RestDslException;

    V getOne(ServiceQuery<K> serviceQuery) throws RestDslException;

    long count(ServiceQuery<K> serviceQuery) throws RestDslException;

    void validateQuery(ServiceQuery<K> query) throws RestDslException;

    ServiceQueryInfo<K> getServiceQueryInfo(ServiceQuery<K> serviceQuery);
}
