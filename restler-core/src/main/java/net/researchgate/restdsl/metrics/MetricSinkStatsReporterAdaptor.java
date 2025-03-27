package net.researchgate.restdsl.metrics;

import net.researchgate.restdsl.dao.MongoBaseServiceDao;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @deprecated Use {@link MetricSink}
 */
@Deprecated(since = "6.1.0", forRemoval = true)
@SuppressWarnings("removal")
public class MetricSinkStatsReporterAdaptor implements MetricSink {
    private static final String QUERY_KEY = "queries.shapes.%s.%%H";
    private final StatsReporter delegate;

    public MetricSinkStatsReporterAdaptor(StatsReporter delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public void timing(MetricName name, long durationMs) {
        delegate.timing(nameToKey(name), durationMs);
    }

    private String nameToKey(MetricName name) {
        if (MongoBaseServiceDao.MONGO_SERVICE_QUERY_METRIC.equals(name.getName())) {
            String collectionName = name.getLabels().get("collectionName");
            String shape = name.getLabels().get("queryShape");
            return String.format(QUERY_KEY, collectionName + "." + shape);
        } else {
            String prefix = name.getName();

            return name.getLabels().entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .flatMap(e -> Stream.of(e.getKey(), e.getValue()))
                    .collect(Collectors.joining(".", prefix + ".", ".%H"));
        }
    }
}
