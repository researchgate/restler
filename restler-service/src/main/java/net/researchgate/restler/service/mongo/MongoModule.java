package net.researchgate.restler.service.mongo;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.mongodb.client.MongoClient;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoModule extends AbstractModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoModule.class);

    private final MongoClient mongoClient;

    private final Datastore datastore;

    public MongoModule(MongoClient mongoClient, String databaseName) {
        this.mongoClient = mongoClient;
        this.datastore = Morphia.createDatastore(mongoClient, databaseName);
    }

    @Override
    protected void configure() {
    }

    @Provides
    MongoClient provideMongoClient() {
        return mongoClient;
    }

    @Provides
    @Singleton
    Datastore provideDataStore() {
        return datastore;
    }
}
