package net.researchgate.restdsl.metrics;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class MetricName {
    private final String name;
    private final Map<String, String> labels;

    public MetricName(String name, Map<String, String> labels) {
        this.name = Objects.requireNonNull(name);
        this.labels = labels != null
                ? Map.copyOf(labels)
                : Collections.emptyMap();
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof MetricName)) return false;

        MetricName that = (MetricName) o;
        return name.equals(that.name) && labels.equals(that.labels);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + labels.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MetricName{" +
                "name='" + name + '\'' +
                ", labels=" + labels +
                '}';
    }
}
