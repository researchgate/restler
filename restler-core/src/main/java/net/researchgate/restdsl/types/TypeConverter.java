package net.researchgate.restdsl.types;

/**
 * Converting string to right type
 */
public interface TypeConverter<T> {
    T deserialize(String val);

    // class on which converter is applicable
    Class<T> getType();

}
