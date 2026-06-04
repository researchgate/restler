package net.researchgate.restler.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Tests for error handling in custom deserializers after migration
 * from ctxt.mappingException() to ctxt.handleUnexpectedToken().
 */
public class DeserializerErrorHandlingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // ---- Helper DTOs ----

    static class DateHolder {
        @JsonDeserialize(using = Rfc3339DateDeserializer.class)
        public Date date;
    }

    static class ObjectIdHolder {
        @JsonDeserialize(using = ObjectIdDeserializer.class)
        public ObjectId id;
    }

    // ---- Rfc3339DateDeserializer: happy path ----

    @Test
    public void testDateDeserializer_validZuluDate() throws Exception {
        DateHolder result = mapper.readValue("{\"date\": \"2024-01-15T10:30:00Z\"}", DateHolder.class);
        assertNotNull(result.date);
    }

    @Test
    public void testDateDeserializer_validDateWithTimezone() throws Exception {
        DateHolder result = mapper.readValue("{\"date\": \"2024-01-15T10:30:00+0100\"}", DateHolder.class);
        assertNotNull(result.date);
    }

    @Test
    public void testDateDeserializer_validDateWithFractionalSeconds() throws Exception {
        DateHolder result = mapper.readValue("{\"date\": \"2024-01-15T10:30:00.123456Z\"}", DateHolder.class);
        assertNotNull(result.date);
    }

    // ---- Rfc3339DateDeserializer: error paths (handleUnexpectedToken) ----

    @Test(expected = MismatchedInputException.class)
    public void testDateDeserializer_rejectsNumericToken() throws Exception {
        mapper.readValue("{\"date\": 12345}", DateHolder.class);
    }

    @Test(expected = MismatchedInputException.class)
    public void testDateDeserializer_rejectsBooleanToken() throws Exception {
        mapper.readValue("{\"date\": true}", DateHolder.class);
    }

    @Test(expected = MismatchedInputException.class)
    public void testDateDeserializer_rejectsObjectToken() throws Exception {
        mapper.readValue("{\"date\": {\"nested\": \"value\"}}", DateHolder.class);
    }

    @Test(expected = MismatchedInputException.class)
    public void testDateDeserializer_rejectsArrayToken() throws Exception {
        mapper.readValue("{\"date\": [1, 2, 3]}", DateHolder.class);
    }

    @Test(expected = com.fasterxml.jackson.databind.JsonMappingException.class)
    public void testDateDeserializer_rejectsInvalidDateString() throws Exception {
        mapper.readValue("{\"date\": \"not-a-date\"}", DateHolder.class);
    }

    @Test
    public void testDateDeserializer_acceptsNullToken() throws Exception {
        DateHolder result = mapper.readValue("{\"date\": null}", DateHolder.class);
        assertNull(result.date);
    }

    // ---- ObjectIdDeserializer: happy path ----

    @Test
    public void testObjectIdDeserializer_validObjectId() throws Exception {
        String validId = new ObjectId().toHexString();
        ObjectIdHolder result = mapper.readValue("{\"id\": \"" + validId + "\"}", ObjectIdHolder.class);
        assertNotNull(result.id);
        assertEquals(validId, result.id.toHexString());
    }

    // ---- ObjectIdDeserializer: error paths (handleUnexpectedToken) ----

    @Test(expected = MismatchedInputException.class)
    public void testObjectIdDeserializer_rejectsNumericToken() throws Exception {
        mapper.readValue("{\"id\": 12345}", ObjectIdHolder.class);
    }

    @Test(expected = MismatchedInputException.class)
    public void testObjectIdDeserializer_rejectsBooleanToken() throws Exception {
        mapper.readValue("{\"id\": false}", ObjectIdHolder.class);
    }

    @Test(expected = MismatchedInputException.class)
    public void testObjectIdDeserializer_rejectsObjectToken() throws Exception {
        mapper.readValue("{\"id\": {\"key\": \"val\"}}", ObjectIdHolder.class);
    }

    @Test(expected = MismatchedInputException.class)
    public void testObjectIdDeserializer_rejectsArrayToken() throws Exception {
        mapper.readValue("{\"id\": [\"a\", \"b\"]}", ObjectIdHolder.class);
    }

    @Test(expected = com.fasterxml.jackson.databind.JsonMappingException.class)
    public void testObjectIdDeserializer_rejectsInvalidObjectIdString() throws Exception {
        mapper.readValue("{\"id\": \"not-a-valid-objectid\"}", ObjectIdHolder.class);
    }

    @Test
    public void testObjectIdDeserializer_acceptsNullToken() throws Exception {
        ObjectIdHolder result = mapper.readValue("{\"id\": null}", ObjectIdHolder.class);
        assertNull(result.id);
    }
}



