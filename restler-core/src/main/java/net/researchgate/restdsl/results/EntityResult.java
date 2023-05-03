package net.researchgate.restdsl.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.ws.rs.core.GenericType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;


/**
 * General container for entity results
 * TODO: introduce redirects in the right manner
 */
@Schema(description = "The response to to a restler query. Depending on the query, exactly one of the sub fields 'list' or 'multimap' is populated")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityResult<T> implements EntityContainer<T> {
    @Schema(description = "If the client query contains no groupBy parameter, then this field is populated")
    private EntityList<T> list;

    @Schema(hidden = true, deprecated = true, description = "This field is never populated in restler")
    private EntityMap<T> map;

    @Schema(description = "If the client query contains a groupBy parameter, then this field is populated", example = "null")
    private EntityMultimap<T> multimap;

    @JsonIgnore
    private EntityContainer<T> currentContainer;

    public EntityResult(List<T> items, Long totalItems) {
        this(new EntityList<>(items, totalItems));
    }

    // for Jackson
    private EntityResult() {
        this.currentContainer = null;
    }

    public EntityResult(EntityList<T> list) {
        Preconditions.checkNotNull(list, "List cannot be null");
        this.list = list;
        this.currentContainer = list;
    }

    public EntityResult(EntityMap<T> map) {
        Preconditions.checkNotNull(map, "Map cannot be null");
        this.map = map;
        this.currentContainer = map;
    }

    public EntityResult(EntityMultimap<T> multimap) {
        Preconditions.checkNotNull(multimap, "Multimap cannot be null");
        this.multimap = multimap;
        this.currentContainer = multimap;
    }

    @Override
    public Iterator<T> iterator() {
        return getCurrentEntityContainer().iterator();
    }

    @Override
    @JsonIgnore
    public Long getTotalItems() {
        return getCurrentEntityContainer().getTotalItems();
    }

    @JsonIgnore
    /* NOTE: use with care to avoid necessary transformation into List. Use as iterable if possible
     */
    public List<T> asList() {
        if (list != null) {
            return list.getItems();
        } else {
            return Lists.newArrayList(this);
        }
    }

    @JsonIgnore
    public boolean isEmpty() {
        return !iterator().hasNext();
    }

    /**
     * Returns null if collection is empty
     *
     * @return just one element from the result
     * Typically used when it's clear that we have at most one result
     */
    @JsonIgnore
    public T getOne() {
        Iterator<T> it = iterator();
        if (!it.hasNext()) {
            return null;
        }
        return it.next();
    }

    public EntityList<T> getList() {
        return list;
    }

    public EntityMap<T> getMap() {
        return map;
    }

    public EntityMultimap<T> getMultimap() {
        return multimap;
    }


    // *** HELPERS ***

    // in case Jackson deserialized EntityResult, currentContainer will be null, need to set it
    private EntityContainer<T> getCurrentEntityContainer() {
        if (currentContainer == null) {
            synchronized (this) {
                if (currentContainer == null) {
                    if (list != null) {
                        currentContainer = list;
                    } else if (map != null) {
                        currentContainer = map;
                    } else if (multimap != null) {
                        currentContainer = multimap;
                    }
                }
            }
        }
        return currentContainer;
    }

    /**
     * need to read response into EntityResult with certain type without a need to subclass EntityResult with specific T
     * One cannot say in java "read this JSON into EntityResult of T, where T is a Publication.
     * Type info is erased at runtime. Therefore Jersey has some helper classes to address such an issue.
     * <p>
     * example usage: EntityResult&lt;Publication&gt; result = response.readEntity(EntityResult.getGenericType(Publication.class));
     * <p>
     * this won't work: EntityResult&lt;Publication&gt; result = response.readEntity(EntityResult.class);
     * LinkedHashMap will be put as T and at some point you will get:
     * java.lang.ClassCastException: java.util.LinkedHashMap cannot be cast to net.researchgate.restler.domain.Publication
     */

    public static <T> GenericType<EntityResult<T>> getGenericType(Class<T> entityClazz) {
        ParameterizedType parameterizedGenericType = new ParameterizedType() {
            public Type[] getActualTypeArguments() {
                return new Type[]{entityClazz};
            }

            public Type getRawType() {
                return EntityResult.class;
            }

            public Type getOwnerType() {
                return EntityResult.class;
            }
        };

        return new GenericType<>(parameterizedGenericType);
    }

}
