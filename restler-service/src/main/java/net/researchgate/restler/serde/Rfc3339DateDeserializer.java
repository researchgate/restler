package net.researchgate.restler.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.researchgate.restdsl.util.ThreadLocalDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

public class Rfc3339DateDeserializer extends JsonDeserializer<Date> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Rfc3339DateDeserializer.class.getName());

    private static final ThreadLocalDateFormat RFC3339_ZULU                    = new ThreadLocalDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final ThreadLocalDateFormat RFC3339_ZULU_FRACTIONAL_SECONDS = new ThreadLocalDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

    private static final ThreadLocalDateFormat RFC3339_TZ                      = new ThreadLocalDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final ThreadLocalDateFormat RFC3339_TZ_FRACTIONAL_SECONDS   = new ThreadLocalDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Override
    public Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException{
        Date date;

        if (jp.getCurrentToken() != JsonToken.VALUE_STRING) {
            LOGGER.info("Could not convert string to date. Expected JsonToken.VALUE_STRING, got " + jp.getCurrentToken() + ".");
            throw ctxt.mappingException("Could not convert string to date. Expected JsonToken.VALUE_STRING, got " + jp.getCurrentToken() + ".");
        }

        String formattedDate = jp.getText();
        if (formattedDate == null) {
            LOGGER.info("Could not convert string to date. Expected JsonToken.VALUE_STRING, got null / " + jp.getCurrentToken() + ".");
            throw ctxt.mappingException("Could not convert string to date. Expected JsonToken.VALUE_STRING, got null / " + jp.getCurrentToken() + ".");
        }
        try {
            date = parseRFC3339Date(formattedDate);
        } catch (ParseException e) {
            LOGGER.info("Could not convert string to date. ", e);
            throw ctxt.mappingException("Could not convert string to date. " + e.getMessage());
        }

        return date;
    }

    public Date parseRFC3339Date(String dateString) throws ParseException, IndexOutOfBoundsException {
        Date d;

        // if there is zulu (aka GMT, aka UTC) time zone, we don't need to do any special parsing.
        if (dateString.endsWith("Z")) {
            try {
                d = RFC3339_ZULU.parse(dateString);
            } catch (ParseException pe) {
                // try again with optional decimals
                ThreadLocalDateFormat f = RFC3339_ZULU_FRACTIONAL_SECONDS;
                f.setLenient(true);
                d = f.parse(dateString);
            }
            return d;
        }

        // step one, split off the timezone.
        String firstPart = dateString.substring(0, dateString.lastIndexOf('+'));
        String secondPart = dateString.substring(dateString.lastIndexOf('+'));

        // step two, remove the colon from the timezone offset
        if (secondPart.contains(":")) {
            secondPart = secondPart.substring(0, secondPart.indexOf(':')) + secondPart.substring(secondPart.indexOf(':') + 1);
            dateString = firstPart + secondPart;
        }

        try {
            d = RFC3339_TZ.parse(dateString);
        } catch (ParseException pe) {
            // try again with optional decimals
            ThreadLocalDateFormat f = RFC3339_TZ_FRACTIONAL_SECONDS;
            f.setLenient(true);
            d = f.parse(dateString);
        }
        return d;
    }
}
