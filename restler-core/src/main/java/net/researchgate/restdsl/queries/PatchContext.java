package net.researchgate.restdsl.queries;

import net.researchgate.restdsl.validation.RestlerPreconditions;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Representing additional information for PATCH operations
 */
public class PatchContext {
    public static final PatchContext DEFAULT_CONTEXT = new PatchContext();

    private Set<String> unsetFields = Collections.emptySet();

    private PatchContext() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PatchContext context = new PatchContext();

        private Builder() {
        }

        public Builder unsetFields(Set<String> unsetFields) {
            RestlerPreconditions.checkNotNull("Null unsetFields are passed to PatchContext", unsetFields);
            context.unsetFields = Collections.unmodifiableSet(unsetFields);
            return this;
        }

        public PatchContext build() {
            return context;
        }
    }

    public Set<String> getUnsetFields() {
        return unsetFields;
    }
}
