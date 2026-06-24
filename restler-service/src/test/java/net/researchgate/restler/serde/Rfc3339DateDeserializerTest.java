package net.researchgate.restler.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Tests for {@link Rfc3339DateDeserializer}, covering happy-path formats
 * and error handling after migration from ctxt.mappingException() to
 * ctxt.reportInputMismatch().
 */
public class Rfc3339DateDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    static class DateHolder {
        @JsonDeserialize(using = Rfc3339DateDeserializer.class)
        public Date date;
    }

    // ---- Happy path ----

    @Test
    public void validZuluDate() throws Exception {
        DateHolder result = mapper.readValue("{\"date\": \"2024-01-15T10:30:00Z\"}", DateHolder.class);
        assertNotNull(result.date);
    }

    @Test
    public void validDateWithTimezone() throws Exception {
        DateHolder result = mapper.readValue("{\"date\": \"2024-01-15T10:30:00+0100\"}", DateHolder.class);
        assertNotNull(result.date);
    }

    @Test
    public void validDateWithFractionalSeconds() throws Exception {
        DateHolder result = mapper.readValue("{\"date\": \"2024-01-15T10:30:00.123456Z\"}", DateHolder.class);
        assertNotNull(result.date);
    }

    // ---- Error paths (reportInputMismatch) ----

    @Test(expected = MismatchedInputException.class)
    public void rejectsNumericToken() throws Exception {
        mapper.readValue("{\"date\": 12345}", DateHolder.class);
    }

    @Test(expected = MismatchedInputException.class)
    public void rejectsBooleanToken() throws Exception {
        mapper.readValue("{\"date\": true}", DateHolder.class);
    }

    @Test(expected = MismatchedInputException.class)
    public void rejectsObjectToken() throws Exception {
        mapper.readValue("{\"date\": {\"nested\": \"value\"}}", DateHolder.class);
    }

    @Test(expected = MismatchedInputException.class)
    public void rejectsArrayToken() throws Exception {
        mapper.readValue("{\"date\": [1, 2, 3]}", DateHolder.class);
    }

    @Test(expected = com.fasterxml.jackson.databind.JsonMappingException.class)
    public void rejectsInvalidDateString() throws Exception {
        mapper.readValue("{\"date\": \"not-a-date\"}", DateHolder.class);
    }

    @Test
    public void acceptsNullToken() throws Exception {
        DateHolder result = mapper.readValue("{\"date\": null}", DateHolder.class);
        assertNull(result.date);
    }
}

