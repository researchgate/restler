package net.researchgate.restler.service.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import net.researchgate.restdsl.dao.RestApiServiceDao;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.queries.ServiceQueryInfo;
import net.researchgate.restler.domain.Publication;
import net.researchgate.restler.service.modules.RestlerServiceModule;

/**
 * Another dao to access remote services that also speak rest-dsl
 */
@Singleton
public class ExternalPublicationDao extends RestApiServiceDao<Publication, Long> {

    @Inject
    public ExternalPublicationDao(@Named(RestlerServiceModule.EXTERNAL_PUBLICATION_SERVICE_URL) String baseUrl) {
        super(baseUrl, Publication.class);
    }

    @Override
    public ServiceQueryInfo<Long> getServiceQueryInfo(ServiceQuery<Long> serviceQuery) {
        return new ServiceQueryInfo<>(serviceQuery, true);
    }
}
