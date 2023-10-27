package net.researchgate.restdsl.dao;

import com.google.common.collect.Lists;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.ModifyOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.query.Query;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.metrics.NoOpStatsReporter;
import net.researchgate.restdsl.metrics.StatsReporter;
import net.researchgate.restdsl.queries.ServiceQuery;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
        Query<V> query = convertToMorphiaQuery(serviceQuery);
        return Math.toIntExact(query.delete(new DeleteOptions()).getDeletedCount());
    }

    @Override
    public V save(V entity) {
        prePersist(entity);
        try {
            datastore.save(entity);
        } catch (DuplicateKeyException e) {
            throw new RestDslException("Duplicate mongo key: " + e.getMessage(), RestDslException.Type.DUPLICATE_KEY);
        }
        return entity;
    }

    @Override
    public V patch(ServiceQuery<K> q, Map<String, Object> patchedFields) throws RestDslException {
        List<UpdateOperator> ops = Lists.newArrayList();
        for (Map.Entry<String, Object> e : patchedFields.entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();
            if (value != null) {
                ops.add(UpdateOperators.set(key, value));
            } else {
                ops.add(UpdateOperators.unset(key));
            }
        }
        return findAndModify(q, ops);
    }

    protected UpdateResult update(ServiceQuery<K> q, List<UpdateOperator> updateOperations) throws RestDslException {
        preUpdate(q, updateOperations);
        Query<V> morphiaQuery = convertToMorphiaQuery(q);
        return morphiaQuery.update(new UpdateOptions().multi(true), updateOperations.toArray(new UpdateOperator[0]));
    }

    protected V findAndModify(ServiceQuery<K> q, List<UpdateOperator> updateOperations) throws RestDslException {
        ModifyOptions options = new ModifyOptions()
                .returnDocument(ReturnDocument.AFTER)
                .upsert(false);

        return findAndModify(q, updateOperations, options);
    }

    @Deprecated
    protected V findAndModify(ServiceQuery<K> q, List<UpdateOperator> updateOperations, boolean oldVersion, boolean createIfMissing) throws RestDslException {
        ModifyOptions options = new ModifyOptions()
                .returnDocument(oldVersion ? ReturnDocument.BEFORE : ReturnDocument.AFTER)
                .upsert(createIfMissing);

        return findAndModify(q, updateOperations, options);
    }

    protected V findAndModify(ServiceQuery<K> q, List<UpdateOperator> updateOperations, ModifyOptions options) throws RestDslException {
        preUpdate(q, updateOperations);
        Query<V> morphiaQuery = convertToMorphiaQuery(q);

        try {
            return morphiaQuery.modify(options, updateOperations.toArray(new UpdateOperator[0]));
        } catch (DuplicateKeyException e) {
            throw new RestDslException("Duplicate mongo key: " + e.getMessage(), RestDslException.Type.DUPLICATE_KEY);
        }
    }

    @Override
    public void prePersist(V entity) {
        // no op
    }

    @Override
    public void preUpdate(ServiceQuery<K> q, List<UpdateOperator> updateOperations) {
        // no op
    }

    @Override
    public void preDelete(ServiceQuery<K> q) {
        // no op
    }

}
