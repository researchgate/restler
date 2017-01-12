package net.researchgate.restdsl.exceptions;

/**
 * Represents general REST DSL exception
 */
public class RestDslException extends RuntimeException {
    //TODO: generialize types, rethink them.
    // TODO: need something like generic conflict state
    public enum Type {
        // DB constraint violation
        DUPLICATE_KEY,

        // Entity provided is not valid
        ENTITY_ERROR,

        // Unknown or implementation error
        GENERAL_ERROR,

        // params supplied via HTTP are invalid
        PARAMS_ERROR,

        // Service query contains errors
        QUERY_ERROR
    }

    // default type
    private Type type = Type.GENERAL_ERROR;

    public RestDslException(String message) {
        super(message);
    }

    public RestDslException(String message, Type type) {
        super(message);
        this.type = type;
    }

    public RestDslException(String message, Throwable cause, Type type) {
        super(message, cause);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

}
