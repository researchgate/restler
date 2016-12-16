package net.researchgate.restler.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.bson.types.ObjectId;

import java.io.IOException;


/**
 * Serializer of Mongo ObjectId
 */
public class ObjectIdSerializer extends JsonSerializer<ObjectId> {

    @Override
    public void serialize(ObjectId value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if (value == null) {
            // do not write anything if the value is null!
            return;
        }
        jgen.writeString(value.toHexString());
    }
}
