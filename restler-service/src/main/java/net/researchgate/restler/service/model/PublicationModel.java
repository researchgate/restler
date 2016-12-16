package net.researchgate.restler.service.model;

import com.google.inject.Inject;
import net.researchgate.restdsl.dao.MongoServiceDao;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.model.ServiceModel;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restler.domain.Publication;
import net.researchgate.restler.service.dao.ExternalPublicationDao;
import net.researchgate.restler.service.dao.PublicationDao;

/**
 * Publication model
 */
public class PublicationModel extends ServiceModel<Publication, Long> {
    private PublicationDao publicationDao;

    @Inject
    private ExternalPublicationDao externalPublicationDao;

    @Inject
    public PublicationModel(PublicationDao publicationDao) {
        super(publicationDao);
        this.publicationDao = publicationDao;
    }

    @Override
    protected MongoServiceDao<Publication, Long> getServiceDao() {
        return publicationDao;
    }

    public Publication getExternalPublication(Long id) throws RestDslException {
        return externalPublicationDao.getOne(ServiceQuery.byId(id));
    }

}
