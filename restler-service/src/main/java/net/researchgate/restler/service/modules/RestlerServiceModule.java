package net.researchgate.restler.service.modules;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import net.researchgate.restler.service.config.RestlerConfig;
import net.researchgate.restler.service.dao.AccountDao;
import net.researchgate.restler.service.dao.ExternalPublicationDao;
import net.researchgate.restler.service.dao.PublicationDao;
import net.researchgate.restler.service.dao.ValidatedEntityDao;
import net.researchgate.restler.service.exceptions.ServiceExceptionMapper;
import net.researchgate.restler.service.model.AccountModel;
import net.researchgate.restler.service.model.PublicationModel;
import net.researchgate.restler.service.model.ValidatedEntityModel;
import net.researchgate.restler.service.mongo.MongoClientBuilder;
import net.researchgate.restler.service.mongo.MongoModule;
import net.researchgate.restler.service.resources.AccountResource;
import net.researchgate.restler.service.resources.HealthResource;
import net.researchgate.restler.service.resources.PublicationResource;
import net.researchgate.restler.service.resources.ValidatedEntityResource;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

public class RestlerServiceModule extends DropwizardAwareModule<RestlerConfig> {

    public static final String EXTERNAL_PUBLICATION_SERVICE_URL = "externalPublicationServiceUrl";

    private void bindExternalPublicationUrl(Binder binder) {
        String url = "http://localhost:8091/restler-service/publications/";
        binder.bind(String.class).annotatedWith(Names.named(EXTERNAL_PUBLICATION_SERVICE_URL)).toInstance(url);
    }

    @Override
    public void configure() {
        Binder binder = binder();
        binder.bind(MetricRegistry.class).toInstance(environment().metrics());
        bindExternalPublicationUrl(binder);


        if (configuration().mongoConfig != null) {
            configureMongo(binder, configuration().mongoConfig);
        }
        environment().jersey().register(ServiceExceptionMapper.class);

        binder.bind(AccountDao.class);
        binder.bind(ExternalPublicationDao.class);
        binder.bind(PublicationDao.class);
        binder.bind(ValidatedEntityDao.class);

        binder.bind(AccountModel.class);
        binder.bind(PublicationModel.class);
        binder.bind(ValidatedEntityModel.class);

        binder.bind(HealthResource.class);

        binder.bind(AccountResource.class);
        binder.bind(PublicationResource.class);
        binder.bind(ValidatedEntityResource.class);
    }

    protected void configureMongo(Binder binder, MongoClientBuilder mongoConfig) {
        binder.install(new MongoModule(mongoConfig.build(environment()), mongoConfig.getDbName()));
    }

    @Provides
    @Named("mongoConfig")
    MongoClientBuilder getMongoConfig() {
        return configuration().mongoConfig;
    }

}
