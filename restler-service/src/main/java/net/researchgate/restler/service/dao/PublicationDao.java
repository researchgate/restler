package net.researchgate.restler.service.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.researchgate.restdsl.dao.MongoServiceDao;
import net.researchgate.restler.domain.Publication;
import dev.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

@Singleton
public class PublicationDao extends MongoServiceDao<Publication, Long> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicationDao.class);
    private static final String X_CLIENT_ID_HEADER_NAME = "X-Client-Id";

    @Context
    private HttpHeaders headers;

    @Inject
    public PublicationDao(Datastore datastore) {
        super(datastore, Publication.class);
    }

    @Override
    public void prePersist(Publication entity) {
        if (headers != null) {
            entity.setLastModificationClientId(headers.getHeaderString(X_CLIENT_ID_HEADER_NAME));
        } else {
            LOGGER.error("No headers set in PublicationDao");
        }
    }
}
