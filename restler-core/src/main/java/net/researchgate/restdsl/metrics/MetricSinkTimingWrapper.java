package net.researchgate.restdsl.metrics;

import java.io.Closeable;
import java.util.Objects;

public class MetricSinkTimingWrapper implements Closeable {
    private final MetricSink sink;
    private final MetricName name;
    private final long time;

    public static MetricSinkTimingWrapper of(MetricSink sink, MetricName name) {
        return new MetricSinkTimingWrapper(sink, name);
    }

    private MetricSinkTimingWrapper(MetricSink sink, MetricName name) {
        this.sink = Objects.requireNonNull(sink);
        this.name = Objects.requireNonNull(name);
        time = System.currentTimeMillis();
    }

    @Override
    public void close() {
        sink.timing(name, System.currentTimeMillis() - time);
    }
}
