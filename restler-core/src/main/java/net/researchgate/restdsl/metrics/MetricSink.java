package net.researchgate.restdsl.metrics;

/**
 * Metric sinks can be used to expose query timings measured by restler to
 * a metric store like Prometheus or similar.
 */
public interface MetricSink {
    /**
     * Adds a measured duration to sink.
     *
     * @param name  Metric name and labels
     * @param durationMs Measured time in milliseconds
     */
    void timing(MetricName name, long durationMs);
}
