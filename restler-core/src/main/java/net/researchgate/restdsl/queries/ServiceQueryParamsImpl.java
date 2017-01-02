package net.researchgate.restdsl.queries;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collections;
import java.util.Set;

/**
 * Generic implementation
 */
public class ServiceQueryParamsImpl implements ServiceQueryParams{
    private int defaultLimit = 10000;
    private Set<String> defaultFields;
    private Multimap<String, Object> defaultCriteria;

    @Override
    public int getDefaultLimit() {
        return defaultLimit;
    }

    @Override
    public Set<String> getDefaultFields() {
        return defaultFields;
    }

    @Override
    public Multimap<String, Object> getDefaultCriteria() {
        return defaultCriteria;
    }
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        ServiceQueryParamsImpl params = new ServiceQueryParamsImpl();

        private Builder() {
        }

        public Builder defaultLimit(int defaultLimit) {
            params.defaultLimit = defaultLimit;
            return this;
        }

        public Builder defaultFields(Set<String> defaultFields) {
            params.defaultFields = Collections.unmodifiableSet(defaultFields);
            return this;
        }

        public Builder defaultCriteria(Multimap<String, Object> defaultCriteria) {
            params.defaultCriteria = defaultCriteria;
            return this;
        }

        public Builder addDefaultCriteriaItem(String key, Iterable<Object> values) {
            if (params.defaultCriteria == null) {
                params.defaultCriteria = ArrayListMultimap.create();
            }
            params.defaultCriteria.putAll(key, values);
            return this;
        }

        public ServiceQueryParams build() {
            return params;
        }

    }

    @Override
    public String toString() {
        return "ServiceQueryParamsImpl{" +
                "defaultLimit=" + defaultLimit +
                ", defaultFields=" + defaultFields +
                ", defaultCriteria=" + defaultCriteria +
                '}';
    }
}
