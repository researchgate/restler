package net.researchgate.restler.service.mongo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoClientBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoClientBuilder.class);

    @NotEmpty
    @JsonProperty
    private String uri;

    @JsonProperty
    private boolean tls = true;

    @NotEmpty
    @JsonProperty
    private String dbName;

    @JsonProperty
    private Integer minConnectionsPerHost;

    @JsonProperty
    private Integer connectionsPerHost;

    @JsonProperty
    private Integer threadsAllowedToBlockForConnectionMultiplier;

    @JsonProperty
    private Integer maxWaitTime;

    @JsonProperty
    private Integer maxConnectionIdleTime;

    @JsonProperty
    private Integer maxConnectionLifeTime;

    @JsonProperty
    private Integer connectTimeout;

    @JsonProperty
    private Integer socketTimeout;

    @JsonProperty
    private Boolean socketKeepAlive;

    @JsonProperty
    private String readPreference;

    public MongoClientBuilder() {
    }

    public MongoClientBuilder(String uri, boolean tls, String dbName, Integer minConnectionsPerHost, Integer connectionsPerHost,
                              Integer threadsAllowedToBlockForConnectionMultiplier, Integer maxWaitTime, Integer maxConnectionIdleTime,
                              Integer maxConnectionLifeTime, Integer connectTimeout, Integer socketTimeout, Boolean socketKeepAlive) {
        this.uri = uri;
        this.tls = tls;
        this.dbName = dbName;
        this.minConnectionsPerHost = minConnectionsPerHost;
        this.connectionsPerHost = connectionsPerHost;
        this.threadsAllowedToBlockForConnectionMultiplier = threadsAllowedToBlockForConnectionMultiplier;
        this.maxWaitTime = maxWaitTime;
        this.maxConnectionIdleTime = maxConnectionIdleTime;
        this.maxConnectionLifeTime = maxConnectionLifeTime;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.socketKeepAlive = socketKeepAlive;
    }

    public String getUri() {
        return uri;
    }

    public boolean useTls() {
        return tls;
    }

    public String getDbName() {
        return dbName;
    }

    public Integer getMinConnectionsPerHost() {
        return minConnectionsPerHost;
    }

    public Integer getConnectionsPerHost() {
        return connectionsPerHost;
    }

    public Integer getThreadsAllowedToBlockForConnectionMultiplier() {
        return threadsAllowedToBlockForConnectionMultiplier;
    }

    public Integer getMaxWaitTime() {
        return maxWaitTime;
    }

    public Integer getMaxConnectionIdleTime() {
        return maxConnectionIdleTime;
    }

    public Integer getMaxConnectionLifeTime() {
        return maxConnectionLifeTime;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public Integer getSocketTimeout() {
        return socketTimeout;
    }

    public Boolean isSocketKeepAlive() {
        return socketKeepAlive;
    }

    public MongoClientBuilder readPreference(String readPreference) {
        this.readPreference = readPreference;
        return this;
    }

    public MongoClient buildUnmanaged() {
        LOGGER.info("Creating new mongo client: {}", uri);

        MongoClientOptions.Builder options = MongoClientOptions.builder();

        if (minConnectionsPerHost != null) {
            options.minConnectionsPerHost(minConnectionsPerHost);
        }
        if (connectionsPerHost != null) {
            options.connectionsPerHost(connectionsPerHost);
        }
        if (threadsAllowedToBlockForConnectionMultiplier != null) {
            options.threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockForConnectionMultiplier);
        }
        if (maxWaitTime != null) {
            options.maxWaitTime(maxWaitTime);
        }
        if (maxConnectionIdleTime != null) {
            options.maxConnectionIdleTime(maxConnectionIdleTime);
        }
        if (maxConnectionLifeTime != null) {
            options.maxConnectionLifeTime(maxConnectionLifeTime);
        }
        if (connectTimeout != null) {
            options.connectTimeout(connectTimeout);
        }
        if (socketTimeout != null) {
            options.socketTimeout(socketTimeout);
        }
        if (socketKeepAlive != null) {
            options.socketKeepAlive(socketKeepAlive);
        }
        if (readPreference != null) {
            options.readPreference(ReadPreference.valueOf(readPreference));
        }

        MongoClientURI mongoClientURI = new MongoClientURI(uri, options);
        return new MongoClient(mongoClientURI);
    }

    public MongoClient build(Environment environment) {
        final MongoClient mongoClient = buildUnmanaged();
        environment.lifecycle().manage(new Managed() {
            @Override
            public void start() throws Exception {
            }

            @Override
            public void stop() throws Exception {
                LOGGER.info("Closing mongo client: {}", mongoClient);
                mongoClient.close();
            }
        });
        return mongoClient;
    }
}
