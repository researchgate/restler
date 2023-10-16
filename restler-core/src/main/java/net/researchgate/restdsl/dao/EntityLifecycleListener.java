package net.researchgate.restdsl.dao;

import dev.morphia.query.updates.UpdateOperator;
import net.researchgate.restdsl.queries.ServiceQuery;
import org.bson.Document;

import java.util.List;


interface EntityLifecycleListener<V, K> {

    void prePersist(V entity);

    /**
     * Pre update.
     *
     * @param q                the service query
     * @param updateOperations the update operations. Add any update operations which should be applied to this list.
     */
    void preUpdate(ServiceQuery<K> q, List<UpdateOperator> updateOperations);

    void preDelete(ServiceQuery<K> q);
}
