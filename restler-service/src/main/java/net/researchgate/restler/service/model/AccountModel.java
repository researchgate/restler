package net.researchgate.restler.service.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.model.ServiceModel;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restler.domain.Account;
import net.researchgate.restler.service.dao.AccountDao;
import net.researchgate.restler.service.exceptions.ServiceException;
import org.bson.types.ObjectId;

import javax.ws.rs.core.Response;

/**
 * Model for managing accounts
 */

@Singleton
public class AccountModel extends ServiceModel<Account, ObjectId> {
    private AccountDao accountDao;

    @Inject
    private PublicationModel publicationModel;


    @Inject
    public AccountModel(AccountDao accountDao) {
        super(accountDao);
        this.accountDao = accountDao;
    }

    public Account changeMentor(ObjectId accountId, ObjectId mentorId) throws RestDslException {
        ServiceQuery<ObjectId> q = ServiceQuery.byId(accountId);
        return accountDao.changeAccountMentor(q, mentorId);
    }

    public int deleteById(ObjectId id) throws RestDslException {
        Account account = getOne(id);
        if (account == null) {
            throw new ServiceException("Account with id '" + id + "' not found", Response.Status.NOT_FOUND);
        }

        if (account.getPublicationUids() != null) {
            ServiceQuery<Long> pubsToDelete = ServiceQuery.byIds(account.getPublicationUids());
            publicationModel.delete(pubsToDelete);
        }
        return accountDao.delete(ServiceQuery.byId(id));
    }
}
