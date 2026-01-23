package net.researchgate.restdsl.metrics;

/**
 * No op
 *
 * @deprecated Use {@link NoOpMetricSink}
 */
@Deprecated(since = "6.1.0", forRemoval = true)
@SuppressWarnings("removal")
public class NoOpStatsReporter implements StatsReporter {
    public static final NoOpStatsReporter INSTANCE = new NoOpStatsReporter();

    private NoOpStatsReporter() {
    }

    @Override
    public void timing(String key, long value, double sampleRate) {

    }

    @Override
    public void increment(String key, int magnitude, double sampleRate) {

    }

    @Override
    public void gauge(String key, double value) {

    }
}
