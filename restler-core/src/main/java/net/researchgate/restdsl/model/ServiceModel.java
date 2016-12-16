package net.researchgate.restdsl.model;

import net.researchgate.restdsl.dao.PersistentServiceDao;
import net.researchgate.restdsl.domain.EntityInfo;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.queries.ServiceQueryInfo;
import net.researchgate.restdsl.results.EntityResult;
import net.researchgate.restdsl.util.BeanUtils;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * General model
 * <p>
 * V - entity type
 * K - entity primary key type
 */
public class ServiceModel<V, K> {
    private PersistentServiceDao<V, K> serviceDao;

    public ServiceModel(PersistentServiceDao<V, K> serviceDao) {
        this.serviceDao = serviceDao;
    }

    protected PersistentServiceDao<V, K> getServiceDao() {
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

    public int delete(ServiceQuery<K> q) throws RestDslException {
        return serviceDao.delete(q);
    }

    public V save(V entity) {
        return serviceDao.save(entity);
    }


    public V patch(V entity) throws RestDslException {
        K idField = EntityInfo.get((Class<V>)entity.getClass()).getIdFieldValue(entity);

        ServiceQuery<K> q = ServiceQuery.byId(idField);
        V oldBean = getOne(idField);
        try {
            Map<String, Object> diff =
                    BeanUtils.shallowDifferences(oldBean, entity, Collections.emptySet(), true, false);
            if (diff.isEmpty()) {
                return oldBean;
            }
            return serviceDao.patch(q, diff);
        } catch (Exception e) {
            throw new RestDslException("Unable to diff the provided entity with the db entity (class " +
                    entity.getClass().getName() + ")", e, RestDslException.Type.ENTITY_ERROR);
        }
    }

    public ServiceQueryInfo<K> getServiceQueryInfo(ServiceQuery<K> q) {
        return serviceDao.getServiceQueryInfo(q);
    }

    // helpers

    // this is a guava Preconditions.checkNotNull inspired helper method
    protected void ensureNotNull(Object value, String fieldName) throws RestDslException {
        if (value == null) {
            throw new RestDslException("Field " + fieldName + "must not be null", RestDslException.Type.ENTITY_ERROR);
        }
    }

    protected void ensureNotSet(Object value, String fieldName) throws RestDslException {
        if (value != null) {
            throw new RestDslException("Field " + fieldName + "must not be set, but got " + value,RestDslException.Type.ENTITY_ERROR);
        }
    }

    // throws an exception if the client provides a value for this field and it is different from the base value
    protected void ensureNotModified(Function<V, ?> getter, V base, V patch) throws RestDslException {
        if (isModified(getter, base, patch)) {
            throw new RestDslException("Cannot set " + getter + " from " + getter.apply(base)
                    + " to " + getter.apply(patch) + " for " + patch, RestDslException.Type.ENTITY_ERROR);
        }
    }

    protected boolean isModified(Function<V, ?> f, V base, V patch) {
        Object patchVal = f.apply(patch);
        return patchVal != null && !patchVal.equals(f.apply(base));
    }




}
