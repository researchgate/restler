package net.researchgate.restdsl.dao;

import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.ServiceQuery;

import java.util.Map;

/**
 * DAO that also can insert, remove, patch entities
 */
public interface PersistentServiceDao<V, K> extends ServiceDao<V, K>, EntityLifecycleListener<V, K> {

    /**
     * @param serviceQuery service query
     * @return number of deleted entries
     */
    int delete(ServiceQuery<K> serviceQuery) throws RestDslException;


    /**
     * @param entity entity to be saved
     * @return returns an entity with possibly augmented/modified values that ended up in the storage
     */
    V save(V entity);


    V patch(ServiceQuery<K> q, Map<String, Object> patchedFields) throws RestDslException;
}
