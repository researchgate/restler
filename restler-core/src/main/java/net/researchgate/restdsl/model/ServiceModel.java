package net.researchgate.restdsl.model;

import net.researchgate.restdsl.dao.PersistentServiceDao;
import net.researchgate.restdsl.domain.EntityInfo;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.PatchContext;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.util.BeanUtils;

import java.util.Collections;
import java.util.Map;

/**
 * This model exposes full CRUD.
 * Use this if you want simply want to expose the mongo operations via REST.
 * If you have more challenging businessLogic, consider using {@link BaseServiceModel} and implement
 * write operations yourself.
 *
 * @param <V> Type of the entity
 * @param <K> Type of the entity's id field
 */
public abstract class ServiceModel<V, K> extends BaseServiceModel<V, K> {
    private final PersistentServiceDao<V, K> serviceDao;

    public ServiceModel(PersistentServiceDao<V, K> serviceDao) {
        super(serviceDao);
        this.serviceDao = serviceDao;
    }

    protected PersistentServiceDao<V, K> getServiceDao() {
        return serviceDao;
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

}
