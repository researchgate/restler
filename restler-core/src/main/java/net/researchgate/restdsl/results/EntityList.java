package net.researchgate.restdsl.results;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;

import java.util.Iterator;
import java.util.List;

/**
 * Entity list - ordered plain collection of entities
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonPropertyOrder({"items", "totalItems"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityList<T> implements EntityContainer<T>{
    private List<T> items;
    private final Long totalItems;

    private EntityList() {
        this.totalItems = null;
    }

    public EntityList(List<T> items, Long totalItems) {
        Preconditions.checkNotNull(items, "Items list cannot be null");
        this.items = items;
        this.totalItems = totalItems;
    }

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }

    public List<T> getItems() {
        return items;
    }

    @Override
    public Long getTotalItems() {
        return totalItems;
    }
}
