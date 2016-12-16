package net.researchgate.restdsl.dao;

import net.researchgate.restdsl.queries.ServiceQuery;
import org.mongodb.morphia.query.UpdateOperations;


interface EntityLifecycleListener<V, K> {

    void prePersist(V entity);

    void preUpdate(ServiceQuery<K> q, UpdateOperations<V> updateOperations);

    void preDelete(ServiceQuery<K> q);
}
