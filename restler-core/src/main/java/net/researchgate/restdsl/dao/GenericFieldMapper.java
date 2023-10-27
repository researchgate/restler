package net.researchgate.restdsl.dao;

import net.researchgate.restdsl.exceptions.RestDslException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

public class GenericFieldMapper implements EntityFieldMapper {

    @Override
    public Class<?> getFieldType(Class<?> cl, String fieldName) {
        try {
            Field field = cl.getDeclaredField(fieldName);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new RestDslException("Field not found: " + fieldName, RestDslException.Type.ENTITY_ERROR);
        }
    }

    @Override
    public Class<?> getNormalizedType(Class<?> cl, String fieldName) {
        try {
            Field field = cl.getDeclaredField(fieldName);
            Class<?> fieldType = field.getType();

            if (List.class.isAssignableFrom(fieldType)) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                return (Class<?>) listType.getActualTypeArguments()[0];
            } else if (Map.class.isAssignableFrom(fieldType)) {
                ParameterizedType mapType = (ParameterizedType) field.getGenericType();
                return (Class<?>) mapType.getActualTypeArguments()[1];
            }

            return fieldType;
        } catch (NoSuchFieldException e) {
            throw new RestDslException("Field not found: " + fieldName, RestDslException.Type.ENTITY_ERROR);
        }
    }

    @Override
    public String getIdFieldName(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(dev.morphia.annotations.Id.class)) {
                return field.getName();
            }
        }
        return null;
    }

    @Override
    public Class<?> getIdFieldClazz(Class<?> clazz) {
        String idFieldName = getIdFieldName(clazz);
        if (idFieldName != null) {
            return getFieldType(clazz, idFieldName);
        }
        return null;
    }

    @Override
    public Object getIdValue(Class<?> clazz, Object instance) {
        String idFieldName = getIdFieldName(clazz);
        if (idFieldName != null) {
            try {
                Field field = clazz.getDeclaredField(idFieldName);
                field.setAccessible(true);
                return field.get(instance);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RestDslException("No id field annotated on " + clazz.getCanonicalName(), RestDslException.Type.ENTITY_ERROR);
            }
        }
        return null;
    }
}
