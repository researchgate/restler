package net.researchgate.restdsl.results;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents results in the form of mapping between some ids and entities
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonPropertyOrder({"items", "totalItems"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityMap<T> implements EntityContainer<T> {
    private Map<Object, T> items;
    private final Long totalItems;

    private EntityMap() {
        this.totalItems = null;
    }

    public EntityMap(Map<Object, T> items, Long totalItems) {
        Preconditions.checkNotNull(items, "Items map cannot be null");
        this.items = items;
        this.totalItems = totalItems;
    }

    public EntityMap(Collection<T> items, Function<T, Object> keySelector, Long totalItems) {
        Preconditions.checkNotNull(items, "Items list cannot be null");
        this.items = items.stream().collect(Collectors.toMap(keySelector, Function.identity()));
        this.totalItems = totalItems;
    }

    @Override
    public Iterator<T> iterator() {
        return items.values().iterator();
    }

    public Map<Object, T> getItems() {
        return items;
    }

    @Override
    public Long getTotalItems() {
        return totalItems;
    }
}
