package com.elgi.creditsimulator.json;

import com.elgi.creditsimulator.exception.RemoteServiceException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class JsonObject {

    private final Map<String, Object> fields;

    JsonObject(Map<String, Object> fields) {
        this.fields = Collections.unmodifiableMap(
                new LinkedHashMap<>(Objects.requireNonNull(fields, "fields")));
    }

    public Set<String> fieldNames() {
        return fields.keySet();
    }

    public boolean has(String field) {
        return fields.containsKey(field);
    }

    public String getString(String field) {
        Object value = require(field);

        if (!(value instanceof String)) {
            throw wrongType(field, "a string", value);
        }
        return (String) value;
    }

    public BigDecimal getBigDecimal(String field) {
        Object value = require(field);

        if (!(value instanceof BigDecimal)) {
            throw wrongType(field, "a number", value);
        }
        return (BigDecimal) value;
    }

    /**
     * A whole number.
     *
     * <p>Rejects {@code 2025.5} rather than silently truncating it to 2025. A model year with a
     * fraction means the payload is wrong, and rounding it away would hide that.
     */
    public int getInt(String field) {
        BigDecimal value = getBigDecimal(field);

        try {
            return value.intValueExact();
        } catch (ArithmeticException cause) {
            throw new RemoteServiceException(
                    "Field '" + field + "' should be a whole number, but was " + value.toPlainString()
                            + ".");
        }
    }

    private Object require(String field) {
        if (!fields.containsKey(field)) {
            throw new RemoteServiceException(
                    "The service response is missing the field '" + field + "'. It contained: "
                            + fields.keySet() + ".");
        }
        Object value = fields.get(field);
        if (value == null) {
            throw new RemoteServiceException("Field '" + field + "' was null in the service response.");
        }
        return value;
    }

    private RemoteServiceException wrongType(String field, String expected, Object actual) {
        return new RemoteServiceException(
                "Field '" + field + "' should be " + expected + ", but was "
                        + actual.getClass().getSimpleName() + " (" + actual + ").");
    }

    @Override
    public String toString() {
        return fields.toString();
    }

}
