package net.researchgate.restdsl.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ThreadLocalDateFormat {

    private static final String DEFAULT_PATTERN = "yyyy-MM-dd";

    private final String pattern;

    private final ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat(pattern);
        }
    };

    public ThreadLocalDateFormat() {
        this.pattern = DEFAULT_PATTERN;
    }

    public ThreadLocalDateFormat(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    public String format(Date date) {
        return df.get().format(date);
    }

    public Date parse(String source) throws ParseException {
        return df.get().parse(source);
    }

    public void setLenient(boolean lenient) {
        df.get().setLenient(lenient);
    }

}
