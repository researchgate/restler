package net.researchgate.restdsl.model;

import net.researchgate.restdsl.dao.ServiceDao;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.queries.ServiceQueryInfo;
import net.researchgate.restdsl.results.EntityResult;

/**
 * This model implements read only access to the underlying mongo collection.
 * Use this model if you want to make sure that your data is only written to in a controlled way.
 *
 * If you want to simply expose CRUD via REST, use a {@link ServiceModel} instead.
 *
 * @param <V> Type of the entity
 * @param <K> Type of the entity's id field
 */
public abstract class BaseServiceModel<V, K> {
    protected ServiceDao<V, K> serviceDao;

    public BaseServiceModel(ServiceDao<V, K> serviceDao) {
        this.serviceDao = serviceDao;
    }

    protected ServiceDao<V, K> getServiceDao() {
        return serviceDao;
    }

    public EntityResult<V> get(K id) throws RestDslException {
        return serviceDao.get(ServiceQuery.byId(id));
    }

    public V getOne(K id) throws RestDslException {
        return serviceDao.getOne(ServiceQuery.byId(id));
    }

    public EntityResult<V> get(ServiceQuery<K> q) throws RestDslException {
        return serviceDao.get(q);
    }

    public ServiceQueryInfo<K> getServiceQueryInfo(ServiceQuery<K> q) {
        return serviceDao.getServiceQueryInfo(q);
    }
}
