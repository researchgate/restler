package net.researchgate.restdsl.metrics;

public class NoOpMetricSink implements MetricSink {
    public static final NoOpMetricSink INSTANCE = new NoOpMetricSink();

    @Override
    public void timing(MetricName name, long durationMs) {

    }
}
