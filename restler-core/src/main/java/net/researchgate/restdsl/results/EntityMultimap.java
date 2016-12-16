package net.researchgate.restdsl.results;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import java.util.Iterator;
import java.util.Map;

/**
 * Grouped items by some id
 * Note it's mapped to entity lists so that we can have totalCount for each group
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonPropertyOrder({"items", "totalItems"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityMultimap<T> implements EntityContainer<T> {
    private Map<Object, EntityList<T>> items;
    private final Long totalItems;

    private EntityMultimap() {
        this.totalItems = null;
    }

    public EntityMultimap(Map<Object, EntityList<T>> items, Long totalItems) {
        Preconditions.checkNotNull(items, "Items map cannot be null");
        this.items = items;
        this.totalItems = totalItems;
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<Iterator<T>> its = Iterables.transform(items.values(), EntityList::iterator).iterator();
        return Iterators.concat(its);
    }

    public Map<Object, EntityList<T>> getItems() {
        return items;
    }

    @Override
    public Long getTotalItems() {
        return totalItems;
    }
}
