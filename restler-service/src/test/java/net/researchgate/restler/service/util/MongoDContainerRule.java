package net.researchgate.restler.service.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startable;

public class MongoDContainerRule extends ExternalResource implements Startable {

    private static final GenericContainer mongodb = new GenericContainer("mongo:7.0").withExposedPorts(27017);

    private MongoClient client;

    private Datastore datastore;

    private String dbName;


    public MongoDContainerRule(String dbName) {
        this.dbName = dbName;
    }

    @Override
    protected void before() {
        mongodb.start();
        System.setProperty("dw.mongoConfig.uri", uri().toString());
        System.setProperty("dw.mongoConfig.tls", "false");
        System.setProperty("dw.mongoConfig.dbName", dbName);


        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder();
        settingsBuilder.applyConnectionString(uri()).build();

        client = MongoClients.create(settingsBuilder.build());

        datastore = Morphia.createDatastore(client, dbName);
    }

    @Override
    protected void after() {
        client.close();
        mongodb.stop();
    }

    public String ip() {
        return mongodb.getHost();
    }

    public Integer port() {
        return mongodb.getMappedPort(27017);
    }

    public ConnectionString uri() {
        return new ConnectionString("mongodb://" + ip() + ":" + port());
    }

    public MongoClient client() {
        return client;
    }

    public MongoDatabase database() {
        return client.getDatabase(dbName);
    }

    public Datastore getDatastore() {
        return datastore;
    }

    @Override
    public void start() {
        try {
            this.before();
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void stop() {
        this.after();
    }
}
