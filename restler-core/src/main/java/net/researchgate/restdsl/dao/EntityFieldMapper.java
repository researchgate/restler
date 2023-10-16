package net.researchgate.restdsl.dao;

public interface EntityFieldMapper {

    Class<?> getFieldType(Class<?> cl, String fieldName);

    /**
     * Gets the parameterized type of a List or the key type of a Map, e.g.
     *
     * @return the unwrapped type
     */
    Class<?> getNormalizedType(Class<?> cl, String fieldName);

    /**
     * Return any field name annotated with dev.morphia.annotations.Id or Null if none exists
     * @param clazz
     * @return
     */
    String getIdFieldName(Class<?> clazz);

    /**
     * Returns the type class of the id field if it exists, otherwise Null
     * @param clazz
     * @return
     */
    Class<?> getIdFieldClazz(Class<?> clazz);

    Object getIdValue(Class<?> clazz, Object instance);


}
