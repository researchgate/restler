package net.researchgate.restdsl.domain;

import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.types.TypeInfoUtil;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;

import java.util.HashMap;
import java.util.Map;

/**
 * Important information
 */

public class EntityInfo<V> {
    private String idFieldName;
    private Class<?> idFieldClazz;
    private MappedField mappedIdField;
    private static final Map<Class<?>, EntityInfo<?>> CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <V> EntityInfo<V> get(Class<V> clazz) {
        EntityInfo<V> entityInfo = (EntityInfo<V>) CACHE.get(clazz);
        if (entityInfo == null) {
            //TODO: recheck whether it's 100% correct
            synchronized (clazz) {
                entityInfo = (EntityInfo<V>) CACHE.get(clazz);
                if (entityInfo == null) {
                    entityInfo = new EntityInfo<>(clazz);
                    CACHE.put(clazz, entityInfo);
                }
            }
        }
        return entityInfo;
    }

    private EntityInfo(Class<V> clazz) {
        MappedClass mc = TypeInfoUtil.MAPPER.getMappedClass(clazz);
        mappedIdField = mc.getMappedIdField();
        if(mappedIdField == null) {
            throw new RestDslException("No id field annotated on " + clazz.getCanonicalName(), RestDslException.Type.ENTITY_ERROR);
        }
        idFieldName = mappedIdField.getJavaFieldName();
        idFieldClazz = mappedIdField.getConcreteType();
    }

    public String getIdFieldName() {
        return idFieldName;
    }

    public MappedField getMappedIdField() {
        return mappedIdField;
    }

    public Class<?> getIdFieldClazz() {
        return idFieldClazz;
    }

    public <K> K getIdFieldValue(V entity) {
        //noinspection unchecked
        return (K) mappedIdField.getFieldValue(entity);
    }

    public void setIdFieldValue(V entity, Object val) {
        mappedIdField.setFieldValue(entity, val);
    }

}
