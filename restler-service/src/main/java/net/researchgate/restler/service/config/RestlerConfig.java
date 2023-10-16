package net.researchgate.restler.service.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import net.researchgate.restler.service.mongo.MongoClientBuilder;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Main configuration class
 */
public class RestlerConfig extends Configuration {
    @JsonProperty
    @Valid
    public MongoClientBuilder mongoConfig;
}
