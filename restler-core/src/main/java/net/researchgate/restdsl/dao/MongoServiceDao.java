package net.researchgate.restdsl.dao;

import com.mongodb.DuplicateKeyException;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.metrics.NoOpStatsReporter;
import net.researchgate.restdsl.metrics.StatsReporter;
import net.researchgate.restdsl.queries.ServiceQuery;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This dao exposes full CRUD.
 * Use this if you want simply want to expose the mongo operations via REST.
 * If you have more challenging businessLogic, consider using a {@link MongoBaseServiceDao} and implement
 * write operations yourself.
 *
 * @param <V> Type of the entity
 * @param <K> Type of the entity's id field
 */
@SuppressWarnings("WeakerAccess")
public class MongoServiceDao<V, K> extends MongoBaseServiceDao<V, K> implements PersistentServiceDao<V, K> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoServiceDao.class);

    public MongoServiceDao(Datastore datastore, Class<V> entityClazz) {
        this(datastore, entityClazz, NoOpStatsReporter.INSTANCE);
    }

    public MongoServiceDao(Datastore datastore, Class<V> entityClazz, StatsReporter statsReporter) {
        // allow group by in order to be backwards compatible.
        this(datastore, entityClazz, statsReporter, true);
    }

    // This constructor allows clients to migrate to a dao that does not allow groupBy queries
    public MongoServiceDao(Datastore datastore, Class<V> entityClazz, StatsReporter statsReporter, boolean allowGroupBy) {
        super(datastore, entityClazz, statsReporter);
        // allow group by in order to be backwards compatible.
        if (allowGroupBy) {
            setAllowGroupBy();
        }
    }

    @Override
    public int delete(ServiceQuery<K> serviceQuery) throws RestDslException {
        if ((serviceQuery.getCriteria() == null || serviceQuery.getCriteria().isEmpty())
                && CollectionUtils.isEmpty(serviceQuery.getIdList())) {
            throw new RestDslException("Deletion query should either provide ids or criteria", RestDslException.Type.QUERY_ERROR);
        }
        preDelete(serviceQuery);
        return morphiaDao.deleteByQuery(convertToMorphiaQuery(serviceQuery, false)).getN();
    }

    @Override
    public V save(V entity) {
        prePersist(entity);
        try {
            morphiaDao.save(entity);
        } catch (DuplicateKeyException e) {
            throw new RestDslException("Duplicate mongo key: " + e.getMessage(), RestDslException.Type.DUPLICATE_KEY);
        }
        return entity;
    }

    @Override
    public V patch(ServiceQuery<K> q, Map<String, Object> patchedFields) throws RestDslException {
        UpdateOperations<V> ops = createUpdateOperations();
        for (Map.Entry<String, Object> e : patchedFields.entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();
            if (value != null) {
                ops.set(key, value);
            } else {
                ops.unset(key);
            }
        }

        return findAndModify(q, ops);
    }

    protected UpdateResults update(ServiceQuery<K> q, UpdateOperations<V> updateOperations) throws RestDslException {
        preUpdate(q, updateOperations);
        return morphiaDao.update(convertToMorphiaQuery(q, false), updateOperations);
    }

    protected V findAndModify(ServiceQuery<K> q, UpdateOperations<V> updateOperations) throws RestDslException {
        FindAndModifyOptions options = new FindAndModifyOptions()
                .returnNew(true)
                .upsert(false);

        return findAndModify(q, updateOperations, options);
    }

    @Deprecated
    protected V findAndModify(ServiceQuery<K> q, UpdateOperations<V> updateOperations, boolean oldVersion, boolean createIfMissing) throws RestDslException {
        FindAndModifyOptions options = new FindAndModifyOptions()
                .returnNew(!oldVersion)
                .upsert(createIfMissing);

        return findAndModify(q, updateOperations, options);
    }

    protected V findAndModify(ServiceQuery<K> q, UpdateOperations<V> updateOperations, FindAndModifyOptions options) throws RestDslException {
        preUpdate(q, updateOperations);
        Query<V> morphiaQuery = convertToMorphiaQuery(q, false);
        try {
            return morphiaDao.getDatastore().findAndModify(morphiaQuery, updateOperations, options);
        } catch (DuplicateKeyException e) {
            throw new RestDslException("Duplicate mongo key: " + e.getMessage(), RestDslException.Type.DUPLICATE_KEY);
        }
    }

    /**
     * Use when you need to bypass restDsl, for example  internal operations
     *
     * @return morphia's dao
     * @deprecated Use {@link MongoBaseServiceDao#morphiaDao} instead, as it not unsafe to use morphia. This method will be removed soon because its public!
     */
    public BasicDAO<V, K> getMorphiaDaoUnsafe() {
        return morphiaDao;
    }

    @Override
    public void prePersist(V entity) {
        // no op
    }

    @Override
    public void preUpdate(ServiceQuery<K> q, UpdateOperations<V> updateOperations) {
        // no op
    }

    @Override
    public void preDelete(ServiceQuery<K> q) {
        // no op
    }

}
