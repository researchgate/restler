package net.researchgate.restdsl.types;

import com.google.common.base.Converter;
import com.google.common.base.Enums;
import com.google.common.base.Splitter;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.ServiceQueryReservedValue;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.types.ObjectId;
import dev.morphia.Morphia;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inferring types
 */
public class TypeInfoUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeInfoUtil.class);

    public static final Mapper MAPPER = new Morphia().getMapper();

    private static Map<Class, TypeConverter<?>> converters = new HashMap<>();


    public static void addConverter(TypeConverter<?> converter) {
        if (converters.containsKey(converter.getType())) {
            LOGGER.warn("Converter for class " + converter.getType().getName() + " has been already registered");
        }
        converters.put(converter.getType(), converter);
    }


    //TODO: converters via annotations
    @SuppressWarnings("unchecked")
    // fieldName and parentClazz are needed for potential reflection to get serde info from a field
    public static <K> K getValue(String strVal, String fieldName, Class<K> clazz, Class<?> parentClazz) throws RestDslException {
        try {
            ServiceQueryReservedValue reservedValue = ServiceQueryReservedValue.fromString(strVal);
            if (reservedValue != null) {
                if (reservedValue == ServiceQueryReservedValue.NULL) {
                    return null;
                }
                return (K) reservedValue;
            }
            if (converters.containsKey(clazz)) {
                return (K) converters.get(clazz).deserialize(strVal);
            }

            if (clazz == String.class) {
                return (K) strVal;
            } else if (clazz == Long.class || clazz == long.class) {
                return (K) Long.valueOf(strVal, 10);
            } else if (clazz == Integer.class || clazz == int.class) {
                return (K) Integer.valueOf(strVal, 10);
            } else if (clazz == ObjectId.class) {
                return (K) new ObjectId(strVal);
            } else if (clazz == Boolean.class || clazz == boolean.class) {
                return (K) Boolean.valueOf(strVal);
            } else if (clazz.isEnum()) {
                Class c = clazz;
                Converter<String, K> converter = Enums.stringConverter(c);
                return converter.convert(strVal);
            } else if (clazz == Date.class) {
                return (K) new Date(Long.valueOf(strVal, 10));
            }
        } catch (Exception e) {
            throw new RestDslException("Cannot convert '" + strVal + "' to object of class " + clazz.getName(), e, RestDslException.Type.PARAMS_ERROR);
        }
        throw new RestDslException("Unsupported type " + clazz.getName() + " for value '" + strVal + "'", RestDslException.Type.PARAMS_ERROR);
    }


    //TODO: get rid of Pair
    public static Pair<Class<?>, Class<?>> getFieldExpressionClazz(Class<?> cl, String fieldExpression) throws RestDslException {
        return getFieldExpressionClazz(cl, null, fieldExpression, Splitter.on('.').splitToList(fieldExpression), 0);
    }

    private static Pair<Class<?>, Class<?>> getFieldExpressionClazz(Class<?> cl, Class<?> prevClazz, String fieldExpression, List<String> fields, int ptr) throws RestDslException {
        if (ptr >= fields.size()) {
            return Pair.of(cl, prevClazz);
        }
        String fieldName = fields.get(ptr);

        //TODO: cache mapped info
        MappedClass mc = MAPPER.getMappedClass(cl);
        MappedField nestedField = mc.getMappedFieldByJavaField(fieldName);
        if (nestedField == null) {
            throw new RestDslException("Cannot find field " + fieldExpression, RestDslException.Type.PARAMS_ERROR);
        }
        Class<?> nestedClazz = nestedField.getType();
        //TODO: better type checking
        if (nestedClazz.isAssignableFrom(List.class)) {
            Type subType = nestedField.getSubType();
            return getFieldExpressionClazz((Class<?>) subType, cl, fieldExpression, fields, ptr + 1);
        } else {
            return getFieldExpressionClazz(nestedClazz, cl, fieldExpression, fields, ptr + 1);
        }
    }
}
