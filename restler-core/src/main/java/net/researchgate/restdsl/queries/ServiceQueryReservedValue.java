package net.researchgate.restdsl.queries;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * $null, $any, $exists, etc
 */
public enum ServiceQueryReservedValue {
    NULL("$null"), 
    ANY("$any"), 
    EXISTS("$exists");

    private final String strVal;
    private final static Map<String, ServiceQueryReservedValue> map = Maps.newHashMap();
    static {
        for (ServiceQueryReservedValue v : values()) {
            map.put(v.strVal, v);
        }
    }

    public static ServiceQueryReservedValue fromString(String strVal) {
        return map.get(strVal);
    }

    ServiceQueryReservedValue(String strVal) {
        this.strVal = strVal;
    }

    public String getStrVal() {
        return strVal;
    }

    @Override
    public String toString() {
        return strVal;
    }
}
