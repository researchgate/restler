package net.researchgate.restdsl.dao;

import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.ServiceQuery;

import java.util.Map;

/**
 * DAO that also can insert, remove, patch entities.
 * This should cover all the updateOperations.
 * If you want to be more restrictive, consider using a smaller superInterface
 *
 *  @param <V>  value entity
 *  @param <K> primary key of the value entity
 */
public interface PersistentServiceDao<V, K> extends BaseServiceDao<V, K>, EntityLifecycleListener<V, K> {

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
