package net.researchgate.restdsl.dao;

/**
 * A dao that also allows delete by id.
 * This dao describes the smallest common subset of operations for a dao that both reads and writes.
 * All other operations such as insert, overwrite, update, ... depend on the useCase.
 *
 * The PersistentServiceDao subInterface in contrast exposes all write operations.
 *
 *
 * @param <V>  value entity
 * @param <K> primary key of the value entity
 */
public interface BaseServiceDao<V, K> extends ServiceDao<V, K> {

    /**
     * Delete the entity by its id
     *
     * @param id the ID of the entity to delete
     * @return the number of deleted items (0 or 1)
     */
    int delete(K id);

}
