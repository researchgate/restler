package net.researchgate.restler.service.mongo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClientFactory;
import com.mongodb.client.MongoClients;
import com.mongodb.event.ConnectionPoolListener;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private String readPreference;

    private List<Codec<?>> codecs;

    public MongoClientBuilder() {
    }

    public MongoClientBuilder(String uri, boolean tls, String dbName, Integer minConnectionsPerHost, Integer connectionsPerHost,
                              Integer maxWaitTime, Integer maxConnectionIdleTime,
                              Integer maxConnectionLifeTime, Integer connectTimeout, Integer socketTimeout) {
        this.uri = uri;
        this.tls = tls;
        this.dbName = dbName;
        this.minConnectionsPerHost = minConnectionsPerHost;
        this.connectionsPerHost = connectionsPerHost;
        this.maxWaitTime = maxWaitTime;
        this.maxConnectionIdleTime = maxConnectionIdleTime;
        this.maxConnectionLifeTime = maxConnectionLifeTime;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
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

    public MongoClientBuilder readPreference(String readPreference) {
        this.readPreference = readPreference;
        return this;
    }

    public MongoClientBuilder codes(List<Codec<?>> codecs) {
        this.codecs = codecs;
        return this;
    }

    public MongoClient buildUnmanaged() {
        MongoClientSettings.Builder settingsBuilder = getOptionsBuilder();

        LOGGER.info("Creating new mongo client: {}, options {}", stripPassword(uri), settingsBuilder);

        return MongoClients.create(settingsBuilder.build());
    }

    protected static String stripPassword(String uri) {
        String saveUri = uri;
        int at = uri.indexOf('@');
        if (at > 0) {
            int col = uri.indexOf(':');
            if (col > 0) {
                saveUri = uri.substring(0, col) + "password" + uri.substring(at);
            }
        }
        return saveUri;
    }
    protected MongoClientSettings.Builder getOptionsBuilder() {
        MongoClientSettings.Builder options = MongoClientSettings.builder();

        options.applyConnectionString(new ConnectionString(uri));

        options.applyToConnectionPoolSettings(b -> {
            if (minConnectionsPerHost != null) {
                b.minSize(minConnectionsPerHost);
            }
            if (connectionsPerHost != null) {
                b.maxSize(connectionsPerHost);
            }
            if (maxWaitTime != null) {
                b.maxWaitTime(maxWaitTime, TimeUnit.MILLISECONDS);
            }
            if (maxConnectionIdleTime != null) {
                b.maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS);
            }
            if (maxConnectionLifeTime != null) {
                b.maxConnectionLifeTime(maxConnectionLifeTime, TimeUnit.MILLISECONDS);
            }
        });

        options.applyToSocketSettings(b -> {
            if (connectTimeout != null) {
                b.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
            }
            if (socketTimeout != null) {
                b.readTimeout(socketTimeout, TimeUnit.MILLISECONDS);
            }
        });

        if (readPreference != null) {
            options.readPreference(ReadPreference.valueOf(readPreference));
        }

        if (codecs != null) {
            CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    CodecRegistries.fromCodecs(codecs));
            options.codecRegistry(codecRegistry);
        }

        return options;
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
