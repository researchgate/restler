package net.researchgate.restdsl.domain;
//
//import dev.morphia.Datastore;
//import dev.morphia.mapping.codec.pojo.EntityModel;
//import dev.morphia.mapping.codec.pojo.PropertyModel;
import net.researchgate.restdsl.dao.EntityFieldMapper;
import net.researchgate.restdsl.exceptions.RestDslException;

import java.util.HashMap;
import java.util.Map;

/**
 * Important information
 */

public class EntityInfo<V> {
    private final String idFieldName;
    private final Class<?> idFieldClazz;
    private static final Map<Class<?>, EntityInfo<?>> CACHE = new HashMap<>();

    private final EntityFieldMapper mapper;
    private final Class<V> entityClass;

    @SuppressWarnings("unchecked")
    public static <V> EntityInfo<V> get(EntityFieldMapper mapper, Class<V> clazz) {
        EntityInfo<V> entityInfo = (EntityInfo<V>) CACHE.get(clazz);
        if (entityInfo == null) {
            //TODO: recheck whether it's 100% correct
            synchronized (clazz) {
                entityInfo = (EntityInfo<V>) CACHE.get(clazz);
                if (entityInfo == null) {
                    entityInfo = new EntityInfo<>(mapper, clazz);
                    CACHE.put(clazz, entityInfo);
                }
            }
        }
        return entityInfo;
    }

    private EntityInfo(EntityFieldMapper mapper, Class<V> clazz) {
        idFieldName = mapper.getIdFieldName(clazz);
        if (idFieldName == null) {
            throw new RestDslException("No id field annotated on " + clazz.getCanonicalName(), RestDslException.Type.ENTITY_ERROR);
        }
        idFieldClazz = mapper.getIdFieldClazz(clazz);
        this.mapper = mapper;
        this.entityClass = clazz;
    }

    public String getIdFieldName() {
        return idFieldName;
    }

    public Class<?> getIdFieldClazz() {
        return idFieldClazz;
    }

    public <K> K getIdFieldValue(V entity) {
        //noinspection unchecked
        return (K) mapper.getIdValue(entityClass, entity);
    }

}
