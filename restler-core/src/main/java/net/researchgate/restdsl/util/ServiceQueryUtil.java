package net.researchgate.restdsl.util;

import com.google.common.base.Splitter;
import net.researchgate.restdsl.exceptions.RestDslException;

import java.util.List;

/**
 * Different utilities to manage
 */
public class ServiceQueryUtil {
    private static final String CRITERIA_SEPARATOR = "__";

    public static class ParsedQueryField {
        private String fullCriteria;
        private String fieldName;

        private ParsedQueryField(String fullCriteria, String fieldName) {
            this.fullCriteria = fullCriteria;
            this.fieldName = fieldName;
        }

        public String getFullCriteria() {
            return fullCriteria;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    public static ParsedQueryField parseQueryField(String providedField) {

        if (providedField.contains(CRITERIA_SEPARATOR)) {
            // supporting cases like 'rating__gte=20' to avoid URL encoding while testing from the browser URL bar
            List<String> split = Splitter.on(CRITERIA_SEPARATOR).splitToList(providedField);
            if (split.size() != 2) {
                throw new RestDslException("Field '" + providedField + "' must not contain more than 1 separator", RestDslException.Type.PARAMS_ERROR);
            }
            String op = parseCriteriaOperation(split.get(1));
            return new ParsedQueryField(split.get(0) + " " + op, split.get(0));
        } else {
            return new ParsedQueryField(providedField, getPlainFieldName(providedField));
        }
    }

    private static String parseCriteriaOperation(String op) {
        switch (op) {
            case "gt":
                return ">";
            case "gte":
                return ">=";
            case "lt":
                return "<";
            case "lte":
                return "<=";
            case "ne":
                return "<>";
            default:
                throw new RestDslException("Unsupported operation: " + op, RestDslException.Type.PARAMS_ERROR);
        }
    }

    private static String getPlainFieldName(String criteriaKey) {
        return Splitter.on(' ').split(criteriaKey).iterator().next();
    }

}
