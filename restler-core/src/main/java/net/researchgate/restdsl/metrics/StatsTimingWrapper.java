package net.researchgate.restdsl.metrics;

import java.io.Closeable;

public class StatsTimingWrapper implements Closeable {
    private final StatsReporter statsReporter;
    private final String key;
    private final long time;

    public static StatsTimingWrapper of(StatsReporter statsService, String key) {
        return new StatsTimingWrapper(statsService, key);
    }

    private StatsTimingWrapper(StatsReporter statsReporter, String key) {
        this.statsReporter = statsReporter;
        this.key = key;
        time = System.currentTimeMillis();
    }

    @Override
    public void close() {
        statsReporter.timing(key, System.currentTimeMillis() - time);
    }
}
