package net.researchgate.restler.service.mongo;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.mongodb.MongoClient;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoModule extends AbstractModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoModule.class);

    private final MongoClient mongoClient;
    private final Morphia morphia;
    private final String databaseName;

    public MongoModule(MongoClient mongoClient, String databaseName) {
        this.mongoClient = mongoClient;
        this.morphia = new Morphia();
        this.databaseName = databaseName;
    }

    @Override
    protected void configure() {
    }

    @Provides
    MongoClient provideMongoClient() {
        return mongoClient;
    }

    @Provides
    Morphia provideMorphia() {
        LOGGER.info("Providing morphia object");
        return morphia;
    }

    @Provides
    @Singleton
    Datastore provideDataStore() {
        LOGGER.info("Creating new morphia datastore: {}, {}", databaseName, mongoClient.getAddress());
        return morphia.createDatastore(mongoClient, databaseName);
    }
}
