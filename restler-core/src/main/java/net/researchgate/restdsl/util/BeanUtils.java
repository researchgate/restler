package net.researchgate.restdsl.util;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


//TODO: prettify
public final class BeanUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeanUtils.class);

    private BeanUtils() {
    }

    /**
     * Copies the values of the merge Properties from the right hand side bean to the left side if the left bean's value
     * is null. Properties that don't have a getter or a setter will be ignored.
     *
     * @param target          target
     * @param from            from
     * @param mergeProperties merge props
     * @throws Exception when sth. went wrong
     */
    public static <T> boolean merge(T target, T from, Set<String> mergeProperties) throws Exception {
        return merge(target, from, mergeProperties, NewValueNotNullAndOldValueIsNullPredicate.INSTANCE);
    }


    /**
     * Copies the values of the merge Properties from the right hand side bean to the left side if the copyPredicate returns
     * true. Properties that don't have a getter or a setter will be ignored.
     * The triple of the predicate has: The name of the property, the old value and the new value
     * @param target          target
     * @param from            from
     * @param mergeProperties merge props
     * @param copyPredicate the field will be copy if this predicate returns true
     * @throws Exception when sth. went wrong
     */
    public static <T> boolean merge(T target, T from, Set<String> mergeProperties, Predicate<PropertyChange> copyPredicate) throws Exception {
        LOGGER.debug("* merging target '{}' with data from '{}'", target, from);
        BeanInfo beanInfo = Introspector.getBeanInfo(target.getClass());

        boolean updated = false;

        // Iterate over all the attributes
        for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
            String propertyName = descriptor.getName();
            if (mergeProperties != null && !mergeProperties.contains(propertyName)) {
                // LOGGER.debug("+ skipping property '{}' which is not in mergeProperties", propertyName);
                continue;
            }

            Method readMethod = descriptor.getReadMethod();
            Method writeMethod = descriptor.getWriteMethod();
            if (readMethod == null || writeMethod == null) {
                // LOGGER.debug("+ skipping property '{}' with getter '{}' and setter '{}'", new Object[] {
                // propertyName, readMethod, writeMethod });
                continue;
            }

            // LOGGER.debug("+ merging property '{}': getter='{}', setter='{}'", new Object[] { propertyName,
            // readMethod, writeMethod });
            Object originalValue = readMethod.invoke(target);
            Object newValue = readMethod.invoke(from);

            if (copyPredicate.apply(new PropertyChange(propertyName, originalValue, newValue))) {
                writeMethod.invoke(target, newValue);
                updated = true;
            }
        }
        return updated;
    }

    public static <T> boolean deepMerge(T target, T from, Map<String, MergeConfig> mergeProperties) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(target.getClass());

        boolean updated = false;

        boolean includeAllKeys = mergeProperties.containsKey("*");

        // Iterate over all the attributes
        for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
            String propertyName = descriptor.getName();
            if (!includeAllKeys && !mergeProperties.keySet().contains(propertyName)) {
                // LOGGER.debug("+ skipping property '{}' which is not in mergeProperties", propertyName);
                continue;
            }


            Method readMethod = descriptor.getReadMethod();
            Method writeMethod = descriptor.getWriteMethod();
            if (readMethod == null || writeMethod == null) {
                // LOGGER.debug("+ skipping property '{}' with getter '{}' and setter '{}'", new Object[] {
                // propertyName, readMethod, writeMethod });
                continue;
            }

            // LOGGER.debug("+ merging property '{}': getter='{}', setter='{}'", new Object[] { propertyName,
            // readMethod, writeMethod });
            Object originalValue = readMethod.invoke(target);
            Object newValue = readMethod.invoke(from);


            MergeConfig propertyConfig = mergeProperties.get(propertyName);
            Predicate<PropertyChange> copyPredicate = getCopyPredicate(propertyConfig);

            if (copyPredicate.apply(new PropertyChange(propertyName, originalValue, newValue))) {
                if (originalValue != null && propertyConfig != null && propertyConfig.getKeys() != null) {
                    if (deepMerge(originalValue, newValue, propertyConfig.getKeys())) {
                        updated = true;
                    }
                }
                else {
                    writeMethod.invoke(target, newValue);
                    updated = true;
                }
            }
        }
        return updated;
    }

    private static Predicate<PropertyChange> getCopyPredicate(MergeConfig propertyConfig) {
        Predicate<PropertyChange> copyPredicate;

        if (propertyConfig != null) {
            if (propertyConfig.getCopyPredicate() != null) {
                copyPredicate = propertyConfig.getCopyPredicate();
            }
            else if (propertyConfig.getKeys() != null) {
                copyPredicate = NewValueNotNullPredicate.INSTANCE;
            } else {
                copyPredicate = NewValueNotNullAndOldValueIsNullPredicate.INSTANCE;
            }
        } else {
            copyPredicate = NewValueNotNullAndOldValueIsNullPredicate.INSTANCE;
        }
        return copyPredicate;
    }

    private static class NewValueNotNullAndOldValueIsNullPredicate implements Predicate<PropertyChange> {

        public static final NewValueNotNullAndOldValueIsNullPredicate INSTANCE = new NewValueNotNullAndOldValueIsNullPredicate();

        @Override
        public boolean apply(PropertyChange input) {
            return input.getNewValue() != null && input.getOldValue() == null;
        }
    }

    private static class NewValueNotNullPredicate implements Predicate<PropertyChange> {

        public static final NewValueNotNullPredicate INSTANCE = new NewValueNotNullPredicate();

        @Override
        public boolean apply(PropertyChange input) {
            return input.getNewValue() != null;
        }
    }

    public static class MergeConfig {

        private Map<String, MergeConfig> keys;
        private Predicate<PropertyChange> copyPredicate;

        public MergeConfig(Map<String, MergeConfig> keys) {
            this(keys, null);
        }

        public MergeConfig(Map<String, MergeConfig> keys, Predicate<PropertyChange> copyPredicate) {
            this.keys = keys;
            this.copyPredicate = copyPredicate;
        }

        public Map<String, MergeConfig> getKeys() {
            return keys;
        }

        public Predicate<PropertyChange> getCopyPredicate() {
            return copyPredicate;
        }
    }

    public static class MergeConfigBuilder {

        private Map<String, MergeConfig> keys = new HashMap<>();
        private Predicate<PropertyChange> copyPredicate;

        public MergeConfigBuilder addKey(String key, MergeConfig config) {
            keys.put(key, config);
            return this;
        }

        public MergeConfigBuilder addKey(String key) {
            return addKey(key, null);
        }

        public MergeConfigBuilder withCopyPredicate(Predicate<PropertyChange> copyPredicate) {
            this.copyPredicate = copyPredicate;
            return this;
        }

        public MergeConfig build() {
            return new MergeConfig(keys.isEmpty() ? null : keys, copyPredicate);
        }

    }

    public static class PropertyChange {

        private String property;
        private Object oldValue;
        private Object newValue;

        public PropertyChange(String property, Object oldValue, Object newValue) {
            this.property = property;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getProperty() {
            return property;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }
    }

    /**
     * Retrieves an objects value by dot notation.
     *
     * @param obj      (The object to search within for the given property)
     * @param property (dot separated hierarchy of fields)
     * @return The requested object's value
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Object retrieveObjectValue(Object obj, String property) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        if (property.contains(".")) {
            // We need to recurse down to the final object
            String props[] = property.split("\\.");
            Method method = obj.getClass().getMethod(getGetterMethodName(props[0], false));
            Object value = method.invoke(obj);
            if (value == null) {
                return null;
            }
            return retrieveObjectValue(value, property.substring(props[0].length() + 1));
        } else {
            Method method = obj.getClass().getMethod(getGetterMethodName(property, false));
            return method.invoke(obj);
        }
    }

    public static Method getWriteMethod(Object obj, String propertyName) throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());

        for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
            if (descriptor.getName().equals(propertyName)) {
                return descriptor.getWriteMethod();
            }
        }

        return null;
    }

    /**
     * Set an objects value by dot notation.
     *
     * @param obj
     * @param property (dot separated hierarchy of fields)
     * @param newValue
     * @return
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Object setObjectValue(Object obj, String property, Object newValue) throws SecurityException, NoSuchMethodException,
            IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, IntrospectionException {

        if (!property.contains(".")) {
            Method method = getWriteMethod(obj, property);
            method.setAccessible(true);
            return method.invoke(obj, newValue);
        }

        // We need to recurse down to the final object
        String props[] = property.split("\\.");
        Method method = obj.getClass().getMethod(getGetterMethodName(props[0], false));
        Object value = method.invoke(obj);
        if (value == null) {
            return null;
        }

        if (!(value instanceof List)) {
            return setObjectValue(value, property.substring(props[0].length() + 1), newValue);
        }
        Integer listIndex = Integer.parseInt(props[1]);
        // multi-value object processing
        List multiValuedObj = ((List) value);

        // check if the request is to set new value to the item itself in the list
        if (props.length == 2) {
            multiValuedObj.set(listIndex, newValue);
            return null;
        }
        // recall over the specific item in the array
        int beginIndex = props[0].length() + 1 + props[1].length() + 1;
        return setObjectValue(multiValuedObj.get(listIndex), property.substring(beginIndex), newValue);
    }

    /**
     * Set an objects value by dot notation.
     *
     * @param obj
     * @param property
     *            (dot separated hierarchy of fields)
     * @param newValueType
     * @param newValue
     * @return
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @Deprecated
    public static Object setObjectValue(Object obj, String property, Class<?> newValueType, Object newValue) throws SecurityException, NoSuchMethodException,
            IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        if (property.contains(".")) {
            // We need to recurse down to the final object
            String props[] = property.split("\\.");
            Method method = obj.getClass().getMethod(getGetterMethodName(props[0], false));
            Object value = method.invoke(obj);
            if (value == null) {
                return null;
            }
            return setObjectValue(value, property.substring(props[0].length() + 1), newValueType, newValue);
        } else {
            Method method = obj.getClass().getMethod(getSetterMethodName(property), newValueType);
            method.setAccessible(true);
            return method.invoke(obj, new Object[] { newValue });
        }
    }

    /**
     * Unsets the field in an object using dot notation.  If the property description is for a field within a list, it will traverse into
     * the list and unset the appropriate fields
     * @param obj
     * @param property
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws IntrospectionException
     */
    public static void unsetObjectFields(Object obj, String property) throws SecurityException, NoSuchMethodException,
            IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, IntrospectionException {

        if (!property.contains(".")) {

            java.lang.reflect.Field prop;
            try {
                prop = obj.getClass().getDeclaredField(property);
            } catch (NoSuchFieldException e) {
                return;
            }
            if (prop == null) {
                return;
            }
            prop.setAccessible(true);
            prop.set(obj, null);
            return;
        }

        // We need to recurse down to the final object
        String props[] = property.split("\\.");
        Method method = obj.getClass().getMethod(BeanUtils.getGetterMethodName(props[0], false));
        Object value = method.invoke(obj);
        if (value == null) {
            return;
        }

        if (!(value instanceof List)) {
            unsetObjectFields(value, property.substring(props[0].length() + 1));
            return;
        }

        // The property is a field within a list of objects, so apply the unset to each object within the list
        List multiValuedObj = ((List) value);
        int beginIndex = props[0].length() + 1;
        for (Object o : multiValuedObj) {
            unsetObjectFields(o, property.substring(beginIndex));
        }
    }

    private static Integer getInteger(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String getSetterMethodName(String property) {
        StringBuilder sb = new StringBuilder();
        sb.append(property);
        if (Character.isLowerCase(sb.charAt(0))) {
            if (sb.length() == 1 || !Character.isUpperCase(sb.charAt(1))) {
                sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
            }
        }
        sb.insert(0, "set");
        return sb.toString();
    }

    public static String getGetterMethodName(String property, boolean booleanValue) {
        StringBuilder sb = new StringBuilder();
        sb.append(property);
        if (Character.isLowerCase(sb.charAt(0))) {
            if (sb.length() == 1 || !Character.isUpperCase(sb.charAt(1))) {
                sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
            }
        }

        if (booleanValue) {
            sb.insert(0, "is");
        } else {
            sb.insert(0, "get");
        }
        return sb.toString();
    }

    public static <T> T getPropertyValue(Class<T> requiredType, String propertyName, Object instance) {
        if (requiredType == null) {
            throw new IllegalArgumentException("Invalid argument. requiredType must NOT be null!");
        }
        if (propertyName == null) {
            throw new IllegalArgumentException("Invalid argument. PropertyName must NOT be null!");
        }
        if (instance == null) {
            throw new IllegalArgumentException("Invalid argument. Object instance must NOT be null!");
        }
        T returnValue = null;
        try {
            PropertyDescriptor descriptor = new PropertyDescriptor(propertyName, instance.getClass());
            Method readMethod = descriptor.getReadMethod();
            if (readMethod == null) {
                throw new IllegalStateException("Property '" + propertyName + "' of " + instance.getClass().getName() + " is NOT readable!");
            }
            if (requiredType.isAssignableFrom(readMethod.getReturnType())) {
                try {
                    Object propertyValue = readMethod.invoke(instance);
                    returnValue = requiredType.cast(propertyValue);
                } catch (Exception e) {
                    e.printStackTrace(); // unable to invoke readMethod
                }
            }
        } catch (IntrospectionException e) {
            throw new IllegalArgumentException("Property '" + propertyName + "' is NOT defined in " + instance.getClass().getName() + "!", e);
        }
        return returnValue;
    }

    /**
     * Returns a set of non-null fields from a bean. If a property is in the ignoreFields set or does not have a getter
     * it will be ignored.
     *
     * @param bean
     * @param ignoreFields
     * @return set of non-null field names
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static <T> Set<String> getNonNullFields(T bean, Set<String> ignoreFields) throws IntrospectionException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
        Set<String> nonNullFields = new HashSet<>();
        for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
            String propertyName = descriptor.getName();
            Method readMethod = descriptor.getReadMethod();
            if (ignoreFields.contains(propertyName) || readMethod == null) {
                continue;
            }
            Object value = readMethod.invoke(bean);
            if (value != null) {
                nonNullFields.add(propertyName);
            }
        }
        return nonNullFields;
    }

    public static <T> Map<String, Object> shallowDifferences(T oldBean, T newBean) throws Exception {
        Set<String> ignoreProperties = Collections.emptySet();
        return shallowDifferences(oldBean, newBean, ignoreProperties, false, false);
    }


    public static <T> Map<String, Object> shallowDifferences(T oldBean, T newBean, Set<String> ignoreProperties, boolean ignoreNewNulls, boolean ignoreEmptyCollections) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return shallowDifferences(oldBean, newBean, ignoreProperties, ignoreNewNulls, ignoreEmptyCollections, true);
    }

    /**
     * Computes the shallow difference of read/write properties between oldBean (left hand side) and newBean (right hand side).
     * It will compare all properties that are in/not in properties (depends on the value of ignore. If they differ, the value of the newBean (right hand side)
     * will be reported in the result.
     *
     * @param oldBean
     * @param newBean
     * @param properties
     * @param ignoreNewNulls
     * @param ignoreEmptyCollections (set to true if null should be considered the same as an empty collection)
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static <T> Map<String, Object> shallowDifferences(T oldBean, T newBean, Set<String> properties, boolean ignoreNewNulls, boolean ignoreEmptyCollections, boolean ignore) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        LOGGER.debug("* calculating shallow differences between oldBean='{}' and newBean='{}'", oldBean, newBean);

        Map<String, Object> changes = new HashMap<>();
        BeanInfo beanInfo = Introspector.getBeanInfo(oldBean.getClass());

        // Iterate over all the attributes
        for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
            String propertyName = descriptor.getName();
            boolean contains = properties.contains(propertyName);
            if ((contains && ignore) || (!contains && !ignore)) {
                // LOGGER.debug("+ skipping property '{}' which is in properties", propertyName);
                continue;
            }

            Method readMethod = descriptor.getReadMethod();
            Method writeMethod = descriptor.getWriteMethod();
            if (readMethod == null || writeMethod == null) {
                // LOGGER.debug("+ skipping property '{}' with getter '{}' and setter '{}'", new Object[] {
                // propertyName, readMethod, writeMethod });
                continue;
            }

            // LOGGER.debug("+ comparing property '{}': getter='{}', setter='{}'", new Object[] { propertyName,
            // readMethod, writeMethod });
            Object oldValue = readMethod.invoke(oldBean);
            Object newValue = readMethod.invoke(newBean);

            if (ignoreNewNulls && newValue == null) {
                continue;
            }

            if (ignoreEmptyCollections) {
                // null should be treated the same as an empty collection
                if (oldValue != null && oldValue instanceof Collection<?> && newValue == null) {
                    Collection<?> oldCollection = (Collection<?>) oldValue;
                    if (oldCollection.isEmpty()) {
                        continue;
                    }
                } else if (newValue != null && newValue instanceof Collection<?> && oldValue == null) {
                    Collection<?> newCollection = (Collection<?>) newValue;
                    if (newCollection.isEmpty()) {
                        continue;
                    }
                }
            }

            if (!Objects.equal(oldValue, newValue)) {
                changes.put(propertyName, newValue);
            }
        }

        return changes;
    }

    /**
     * Patches bean (left hand side) with all not-null values from patches (right hand side).
     * Takes into account only read-write properties that are not in ignoreProperties.
     * Only touches the bean (left hand side) if the properties are not equal.
     *
     * @param bean
     * @param patches
     * @param ignoreProperties
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static <T> void patch(T bean, T patches, Set<String> ignoreProperties) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        LOGGER.debug("* patching bean='{}' with not-null values from patches='{}'", bean, patches);

        BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());

        // Iterate over all the attributes
        for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
            String propertyName = descriptor.getName();
            if (ignoreProperties.contains(propertyName)) {
                // LOGGER.debug("+ skipping property '{}' which is in ignoreProperties", propertyName);
                continue;
            }

            Method readMethod = descriptor.getReadMethod();
            Method writeMethod = descriptor.getWriteMethod();
            if (readMethod == null || writeMethod == null) {
                // LOGGER.debug("+ skipping property '{}' with getter '{}' and setter '{}'", new Object[] {
                // propertyName, readMethod, writeMethod });
                continue;
            }

            // LOGGER.debug("+ comparing property '{}': getter='{}', setter='{}'", new Object[] { propertyName,
            // readMethod, writeMethod });
            Object oldValue = readMethod.invoke(bean);
            Object newValue = readMethod.invoke(patches);

            if (newValue == null) {
                continue;
            }

            if (!Objects.equal(oldValue, newValue)) {
                writeMethod.invoke(bean, newValue);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationParameter(Annotation annotation, String parameterName, Class<T> type) {
        try {
            Method m = annotation.getClass().getMethod(parameterName);
            Object o = m.invoke(annotation);
            if (o.getClass().getName().equals(type.getName())) {
                return (T) o;
            } else {
                String msg = "Wrong parameter type. Expected: " + type.getName() + " Actual: " + o.getClass().getName();
                throw new RuntimeException(msg);
            }
        } catch (NoSuchMethodException e) {
            String msg = "The specified annotation defines no parameter '" + parameterName + "'.";
            throw new RuntimeException(msg, e);
        } catch (IllegalAccessException e) {
            String msg = "Unable to get '" + parameterName + "' from " + annotation.getClass().getName();
            throw new RuntimeException(msg, e);
        } catch (InvocationTargetException e) {
            String msg = "Unable to get '" + parameterName + "' from " + annotation.getClass().getName();
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Performs a deep copy of one object to the target which should be a new instance.  The 'target' and 'from' objects do not need to be of
     * the same type. i.e. 'target' could be a Publication, and 'from' a StructuredCitation.  Classes within each of the classes that are not directly
     * assignable should have a no-argument constructor available.
     * @param target
     * @param from
     * @param <T>
     * @throws Exception
     */
    public static <T> void deepCopy(T target, T from) throws Exception {
        BeanInfo beanFromInfo = Introspector.getBeanInfo(from.getClass());
        BeanInfo beanToInfo = Introspector.getBeanInfo(target.getClass());

        Map<String, PropertyDescriptor> toPropertyDescriptors = Maps.newHashMap();
        for (PropertyDescriptor descriptor : beanToInfo.getPropertyDescriptors()) {
            toPropertyDescriptors.put(descriptor.getName(), descriptor);
        }

        for (PropertyDescriptor descriptor : beanFromInfo.getPropertyDescriptors()) {

            PropertyDescriptor toPropertyDesc = toPropertyDescriptors.get(descriptor.getName());
            if (toPropertyDesc == null) {
                continue;
            }

            Method fromReadMethod = descriptor.getReadMethod();
            Method toWriteMethod = toPropertyDesc.getWriteMethod();
            Method toReadMethod = toPropertyDesc.getReadMethod();
            if (fromReadMethod == null || toWriteMethod == null) {
                continue;
            }

            Object newValue = fromReadMethod.invoke(from);
            if (newValue == null) {
                continue;
            }

            if (fromReadMethod.getReturnType().isAssignableFrom(toReadMethod.getReturnType())) {
                toWriteMethod.invoke(target, newValue);
            } else {
                // The types are not compatible so create a new instance of the target type and apply the deep merge using that
                Object instance = toReadMethod.getReturnType().newInstance();
                deepCopy(instance, newValue);
                toWriteMethod.invoke(target, instance);
            }
        }
    }

}
