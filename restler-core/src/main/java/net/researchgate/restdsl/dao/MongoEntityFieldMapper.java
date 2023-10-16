package net.researchgate.restdsl.dao;

import dev.morphia.Datastore;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import net.researchgate.restdsl.exceptions.RestDslException;

public class MongoEntityFieldMapper implements EntityFieldMapper {

    private final Mapper mapper;

    public MongoEntityFieldMapper(Datastore datastore) {
        this.mapper = datastore.getMapper();
    }
    @Override
    public Class<?> getFieldType(Class<?> cl, String fieldName) {
        PropertyModel nestedField = getPropertyModel(cl, fieldName);
        return nestedField.getType();
    }

    @Override
    public Class<?> getNormalizedType(Class<?> cl, String fieldName) {
        PropertyModel nestedField = getPropertyModel(cl, fieldName);
        return nestedField.getNormalizedType();
    }

    @Override
    public String getIdFieldName(Class<?> clazz) {
        EntityModel entityModel = mapper.getEntityModel(clazz);
        PropertyModel idProperty = entityModel.getIdProperty();
        if (idProperty == null) {
            return null;
       }
        return idProperty.getName();
    }

    @Override
    public Class<?> getIdFieldClazz(Class<?> clazz) {
        EntityModel entityModel = mapper.getEntityModel(clazz);
        return entityModel.getIdProperty().getType();
    }

    private PropertyModel getPropertyModel(Class<?> cl, String fieldName) {
        EntityModel entityModel = mapper.getEntityModel(cl);
        PropertyModel nestedField = entityModel.getProperty(fieldName);
        if (nestedField == null) {
            throw new RestDslException("Cannot find field " + fieldName, RestDslException.Type.PARAMS_ERROR);
        }
        return nestedField;
    }

    public Object getIdValue(Class<?> clazz, Object instance) {
        EntityModel entityModel = mapper.getEntityModel(clazz);
        return entityModel.getIdProperty().getValue(instance);
    }
}
