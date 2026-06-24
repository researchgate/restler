package net.researchgate.restler.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.bson.types.ObjectId;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link ObjectIdDeserializer}, covering the happy path
 * and error handling after migration from ctxt.mappingException() to
 * ctxt.reportInputMismatch().
 */
public class ObjectIdDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    static class ObjectIdHolder {
        @JsonDeserialize(using = ObjectIdDeserializer.class)
        public ObjectId id;
    }

    // ---- Happy path ----

    @Test
    public void validObjectId() throws Exception {
        String validId = new ObjectId().toHexString();
        ObjectIdHolder result = mapper.readValue("{\"id\": \"" + validId + "\"}", ObjectIdHolder.class);
        assertNotNull(result.id);
        assertEquals(validId, result.id.toHexString());
    }

    // ---- Error paths (reportInputMismatch) ----

    @Test(expected = MismatchedInputException.class)
    public void rejectsNumericToken() throws Exception {
        mapper.readValue("{\"id\": 12345}", ObjectIdHolder.class);
    }

    @Test(expected = MismatchedInputException.class)
    public void rejectsBooleanToken() throws Exception {
        mapper.readValue("{\"id\": false}", ObjectIdHolder.class);
    }

    @Test(expected = MismatchedInputException.class)
    public void rejectsObjectToken() throws Exception {
        mapper.readValue("{\"id\": {\"key\": \"val\"}}", ObjectIdHolder.class);
    }

    @Test(expected = MismatchedInputException.class)
    public void rejectsArrayToken() throws Exception {
        mapper.readValue("{\"id\": [\"a\", \"b\"]}", ObjectIdHolder.class);
    }

    @Test(expected = com.fasterxml.jackson.databind.JsonMappingException.class)
    public void rejectsInvalidObjectIdString() throws Exception {
        mapper.readValue("{\"id\": \"not-a-valid-objectid\"}", ObjectIdHolder.class);
    }

    @Test
    public void acceptsNullToken() throws Exception {
        ObjectIdHolder result = mapper.readValue("{\"id\": null}", ObjectIdHolder.class);
        assertNull(result.id);
    }
}

