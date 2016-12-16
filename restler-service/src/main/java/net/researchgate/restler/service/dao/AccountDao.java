package net.researchgate.restler.service.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.researchgate.restdsl.dao.MongoServiceDao;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restler.domain.Account;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.util.Date;

/**
 * Account dao
 */
@Singleton
public class AccountDao extends MongoServiceDao<Account, ObjectId> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountDao.class);

    @Context
    private HttpHeaders headers;

    @Inject
    public AccountDao(Datastore datastore) {
        super(datastore, Account.class);
    }

    @Override
    public void prePersist(Account entity) {
        if (headers != null) {
            LOGGER.info("Calling client is: {}", headers.getHeaderString("X-Client-Id"));
        }
        Date now = new Date();
        entity.setModifiedAt(now);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
    }

    @Override
    public void preUpdate(ServiceQuery<ObjectId> q, UpdateOperations<Account> updateOperations) {
        updateOperations.set("modifiedAt", new Date());
    }

    public Account changeAccountMentor(ServiceQuery<ObjectId> q, ObjectId mentorId) throws RestDslException {
        UpdateOperations<Account> ops = createUpdateOperations();
        ops.set("mentorAccountId", mentorId);
        return findAndModify(q, ops);
    }

}
