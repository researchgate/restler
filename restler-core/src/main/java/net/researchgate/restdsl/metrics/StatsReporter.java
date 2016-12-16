package net.researchgate.restdsl.metrics;

public interface StatsReporter {

    void timing(String key, long value, double sampleRate);

    void increment(String key, int magnitude, double sampleRate);

    void gauge(String key, double value);


    default void timing(String key, long value) {
        timing(key, value, 1.0);
    }

    default void decrement(String key) {
        increment(key, -1, 1.0);
    }

    default void decrement(String key, int magnitude) {
        decrement(key, magnitude, 1.0);
    }

    default void increment(String key) {
        increment(key, 1, 1.0);
    }

    default void increment(String key, int magnitude) {
        increment(key, magnitude, 1.0);
    }

    default void decrement(String key, int magnitude, double sampleRate) {
        increment(key, -magnitude, sampleRate);
    }
}
