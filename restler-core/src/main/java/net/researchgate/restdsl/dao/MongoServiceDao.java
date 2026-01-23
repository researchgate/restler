package net.researchgate.restdsl.dao;

import com.google.common.collect.Lists;
import com.mongodb.DuplicateKeyException;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteError;
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
import net.researchgate.restdsl.metrics.MetricSink;
import net.researchgate.restdsl.metrics.NoOpMetricSink;
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
        this(datastore, entityClazz, NoOpMetricSink.INSTANCE);
    }

    /**
     * Creates a new DAO.
     *
     * @param datastore Morphia datastore to query and update
     * @param entityClazz Class of objects to bind MongoDB documents to
     * @param statsReporter Instrumentation to collect query durations
     *
     * @deprecated Use {@link #MongoServiceDao(Datastore, Class, MetricSink)}
     */
    @Deprecated(since = "6.1.0", forRemoval = true)
    @SuppressWarnings("removal")
    public MongoServiceDao(Datastore datastore, Class<V> entityClazz, net.researchgate.restdsl.metrics.StatsReporter statsReporter) {
        // allow group by in order to be backwards compatible.
        this(datastore, entityClazz, statsReporter, true);
    }

    public MongoServiceDao(Datastore datastore, Class<V> entityClazz, MetricSink metricSink) {
        // allow group by in order to be backwards compatible.
        this(datastore, entityClazz, metricSink, true);
    }

    /**
     * Creates a new DAO.
     *
     * @param datastore Morphia datastore to query and update
     * @param entityClazz Class of objects to bind MongoDB documents to
     * @param statsReporter Instrumentation to collect query durations
     * @param allowGroupBy Whether to allow or deny group by queries
     *
     * @deprecated Use {@link #MongoServiceDao(Datastore, Class, MetricSink, boolean)}
     */
    // This constructor allows clients to migrate to a dao that does not allow groupBy queries
    @Deprecated(since = "6.1.0", forRemoval = true)
    @SuppressWarnings("removal")
    public MongoServiceDao(Datastore datastore, Class<V> entityClazz, net.researchgate.restdsl.metrics.StatsReporter statsReporter, boolean allowGroupBy) {
        super(datastore, entityClazz, statsReporter);
        // allow group by in order to be backwards compatible.
        if (allowGroupBy) {
            setAllowGroupBy();
        }
    }

    public MongoServiceDao(Datastore datastore, Class<V> entityClazz, MetricSink metricSink, boolean allowGroupBy) {
        super(datastore, entityClazz, metricSink);
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
        } catch (DuplicateKeyException|MongoWriteException|MongoBulkWriteException e) {
            throw mapMongoExceptions(e);
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
        } catch (DuplicateKeyException|MongoWriteException|MongoBulkWriteException e) {
            throw mapMongoExceptions(e);
        }
    }

    protected RuntimeException mapMongoExceptions(MongoException e) {
        if (e instanceof DuplicateKeyException) {
            return new RestDslException("Duplicate mongo key: " + e.getMessage(), RestDslException.Type.DUPLICATE_KEY);
        } else if (e instanceof MongoWriteException && (ErrorCategory.DUPLICATE_KEY == ((MongoWriteException) e).getError().getCategory())) {
            return new RestDslException("Duplicate mongo key: " + e.getMessage(), RestDslException.Type.DUPLICATE_KEY);
        } else if (e instanceof MongoBulkWriteException && ((MongoBulkWriteException) e).getWriteErrors().stream().map(WriteError::getCategory).anyMatch(cat -> ErrorCategory.DUPLICATE_KEY == cat)) {
            return new RestDslException("Duplicate mongo key: " + e.getMessage(), RestDslException.Type.DUPLICATE_KEY);
        } else {
            return new RuntimeException(e);
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
