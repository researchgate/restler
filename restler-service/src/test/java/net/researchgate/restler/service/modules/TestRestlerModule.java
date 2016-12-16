package net.researchgate.restler.service.modules;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Environment;
import io.dropwizard.validation.valuehandling.OptionalValidatedValueUnwrapper;
import net.researchgate.restler.service.config.RestlerConfig;
import org.hibernate.validator.HibernateValidator;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;

/**
 * Testing module
 */
public class TestRestlerModule extends RestlerServiceModule  {
    private static final String TEST_CONFIG_FILE = "src/test/resources/config.yaml";

    @Override
    protected Environment getEnvironment() {
        return new Environment("test-restler-service-env", new ObjectMapper(), null, new MetricRegistry(), this.getClass().getClassLoader());
    }

    @Override
    public RestlerConfig getConfiguration()  {
        ObjectMapper objectMapper = Jackson.newObjectMapper();

        ValidatorFactory validatorFactory = Validation
                .byProvider(HibernateValidator.class)
                .configure()
                .addValidatedValueHandler(new OptionalValidatedValueUnwrapper())
                .buildValidatorFactory();


        final ConfigurationFactory<RestlerConfig> configurationFactory =
                new DefaultConfigurationFactoryFactory<RestlerConfig>().create(RestlerConfig.class, validatorFactory.getValidator(), objectMapper, "dw");

        try {
            return configurationFactory.build(new FileConfigurationSourceProvider(), TEST_CONFIG_FILE);
        } catch (Exception e) {
            throw new RuntimeException("Cannot get test configuration", e);
        }
    }

}
