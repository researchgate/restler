package net.researchgate.restdsl.model;

import net.researchgate.restdsl.dao.PersistentServiceDao;
import net.researchgate.restdsl.domain.EntityInfo;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.PatchContext;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.queries.ServiceQueryInfo;
import net.researchgate.restdsl.results.EntityResult;
import net.researchgate.restdsl.util.BeanUtils;

import java.util.Collections;
import java.util.Map;

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


    public V patch(V entity, PatchContext patchContext) throws RestDslException {
        K idField = EntityInfo.get((Class<V>) entity.getClass()).getIdFieldValue(entity);

        ServiceQuery<K> q = ServiceQuery.byId(idField);
        V oldBean = getOne(idField);
        try {
            Map<String, Object> changes =
                    BeanUtils.shallowDifferences(oldBean, entity, Collections.emptySet(), true, false);
            for (String f : patchContext.getUnsetFields()) {
                if (changes.containsKey(f)) {
                    throw new RestDslException("Patched field '" + f + "' is also requested to be unset", RestDslException.Type.PARAMS_ERROR);
                }
                changes.put(f, null);
            }

            if (changes.isEmpty()) {
                return oldBean;
            }

            return serviceDao.patch(q, changes);
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


}
