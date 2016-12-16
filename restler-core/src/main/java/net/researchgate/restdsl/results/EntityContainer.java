package net.researchgate.restdsl.results;

/**
 * Basic methods that an entity container should have
 */
public interface EntityContainer<T> extends Iterable<T> {
    Long getTotalItems();
}
