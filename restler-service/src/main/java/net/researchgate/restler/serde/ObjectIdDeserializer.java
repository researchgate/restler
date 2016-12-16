package net.researchgate.restler.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ObjectIdDeserializer extends JsonDeserializer<ObjectId> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectIdDeserializer.class.getName());

    @Override
    public ObjectId deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        if (jp.getCurrentToken() != JsonToken.VALUE_STRING) {
            LOGGER.info("Could not convert string to ObjectId. Expected JsonToken.VALUE_STRING, got " + jp.getCurrentToken() + ".");
            throw ctxt.mappingException("Could not convert string to ObjectId. Expected JsonToken.VALUE_STRING, got " + jp.getCurrentToken() + ".");
        }
        String objectIdStr = jp.getText();
        return new ObjectId(objectIdStr);
    }
}
