package net.researchgate.restler.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.researchgate.restdsl.util.ThreadLocalDateFormat;

import java.io.IOException;
import java.util.Date;

public class Rfc3339DateSerializer extends JsonSerializer<Date> {

    private final static ThreadLocalDateFormat formatter = new ThreadLocalDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Override
    public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if (value == null) {
            // do not write anything if the value is null!
            return;
        }
        String formattedDate = formatter.format(value);
        jgen.writeString(formattedDate);
    }

}
