package net.researchgate.restdsl.validation;

import net.researchgate.restdsl.exceptions.RestDslException;

import java.util.function.Function;

/**
 * Restler-specific precondtions
 */
public class RestlerPreconditions {
    public static void ensureNotNull(Object value, String fieldName) throws RestDslException {
        if (value == null) {
            throw new RestDslException("Field " + fieldName + " must not be null", RestDslException.Type.ENTITY_ERROR);
        }
    }

    public static void ensureNotSet(Object value, String fieldName) throws RestDslException {
        if (value != null) {
            throw new RestDslException("Field " + fieldName + " must not be set, but got " + value, RestDslException.Type.ENTITY_ERROR);
        }
    }

    // throws an exception if the client provides a value for this field and it is different from the base value
    public static <V> void ensureNotModified(Function<V, ?> getter, V base, V patch) throws RestDslException {
        if (isModified(getter, base, patch)) {
            throw new RestDslException("Cannot set " + getter + " from " + getter.apply(base)
                    + " to " + getter.apply(patch) + " for " + patch, RestDslException.Type.ENTITY_ERROR);
        }
    }

    public static <V> boolean isModified(Function<V, ?> f, V base, V patch) {
        Object patchVal = f.apply(patch);
        return patchVal != null && !patchVal.equals(f.apply(base));
    }

    public static void checkNotNull(String msg, Object value) {
        if (value == null) {
            throw new RestDslException(msg, RestDslException.Type.PARAMS_ERROR);
        }
    }
}
