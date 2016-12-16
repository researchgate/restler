package net.researchgate.restdsl.queries;

/**
 * Object representing info
 */
public class ServiceQueryInfo<K> {
    private ServiceQuery<K> query;
    private String urlPart;
    private boolean safeQuery;

    public ServiceQueryInfo(ServiceQuery<K> query, boolean safeQuery) {
        this.query = query;
        this.urlPart = query.toUrlPart();
        this.safeQuery = safeQuery;
    }

    public ServiceQuery<?> getQuery() {
        return query;
    }

    public String getUrlPart() {
        return urlPart;
    }

    public boolean isSafeQuery() {
        return safeQuery;
    }
}
