package net.researchgate.restler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import net.researchgate.restler.service.config.RestlerConfig;
import net.researchgate.restler.service.modules.RestlerServiceModule;
import org.glassfish.jersey.server.ServerProperties;

import java.io.File;
import java.util.List;

/**
 * Entry-point for the application
 */
public class RestlerApplication extends Application<RestlerConfig> {

    private static List<String> configPlaces = Lists.newArrayList(
            "config/config.yaml",
            "../config/config.yaml"
    );

    @Override
    public String getName() {
        return "restler-service";
    }

    @Override
    public void initialize(Bootstrap<RestlerConfig> bootstrap) {
        super.initialize(bootstrap);

        ObjectMapper mapper = bootstrap.getObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        GuiceBundle<RestlerConfig> guiceBundle = GuiceBundle.defaultBuilder(RestlerConfig.class)
                .modules(Lists.newArrayList(new RestlerServiceModule()))
                .build();
        bootstrap.addBundle(guiceBundle);
    }


    @Override
    public void run(RestlerConfig configuration, Environment environment) throws Exception {
        environment.jersey().getResourceConfig().property(ServerProperties.WADL_FEATURE_DISABLE, false);
    }

    // MAIN ENTRY POINT
    public static void main(String[] args) throws Exception {
        RestlerApplication restlerApplication = new RestlerApplication();

        if (args.length == 0) {
            for (String place : configPlaces) {
                place = place.replaceAll("<service>", restlerApplication.getName());
                File f = new File(place);
                if (f.exists() && !f.isDirectory()) {
                    args = new String[2];
                    args[0] = "server";
                    args[1] = f.getCanonicalPath();
                    break;
                }
            }
        }

        restlerApplication.run(args);
    }
}
