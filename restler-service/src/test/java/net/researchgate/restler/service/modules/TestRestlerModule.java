package net.researchgate.restler.service.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jackson.Jackson;
import net.researchgate.restler.service.config.RestlerConfig;
import net.researchgate.restler.service.mongo.MongoClientBuilder;
import net.researchgate.restler.service.mongo.MongoModule;
import net.researchgate.restler.service.util.MongoDContainerRule;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;

/**
 * Testing module
 */
public class TestRestlerModule extends RestlerServiceModule  {
    private static final String TEST_CONFIG_FILE = "src/test/resources/config.yaml";
    private final MongoDContainerRule mongodb;

    public TestRestlerModule(MongoDContainerRule mongodb) {
        this.mongodb = mongodb;
    }

    @Override
    protected void configureMongo(Binder binder, MongoClientBuilder mongoConfig) {
        binder.install(new MongoModule(mongodb.client(), mongoConfig.getDbName()));
    }

    @Override
    protected Environment environment() {
        return new Environment("test-restler-service-env");
    }

    @Override
    public RestlerConfig configuration()  {
        ObjectMapper objectMapper = Jackson.newObjectMapper();

        ValidatorFactory validatorFactory = Validation
                .buildDefaultValidatorFactory();

        final ConfigurationFactory<RestlerConfig> configurationFactory =
                new DefaultConfigurationFactoryFactory<RestlerConfig>().create(RestlerConfig.class, validatorFactory.getValidator(), objectMapper, "dw");

        try {
            return configurationFactory.build(new FileConfigurationSourceProvider(), TEST_CONFIG_FILE);
        } catch (Exception e) {
            throw new RuntimeException("Cannot get test configuration", e);
        }
    }

}
