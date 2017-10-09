package net.researchgate.restler.service.resources;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.ServiceQueryParams;
import net.researchgate.restdsl.queries.ServiceQueryParamsImpl;
import net.researchgate.restdsl.resources.ServiceResource;
import net.researchgate.restler.domain.Account;
import net.researchgate.restler.domain.AccountStats;
import net.researchgate.restler.service.exceptions.ServiceException;
import net.researchgate.restler.service.model.AccountModel;
import org.bson.types.ObjectId;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Account resource
 */
@Path("/accounts")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class AccountResource extends ServiceResource<Account, ObjectId> {

    private AccountModel accountModel;

    private static final ServiceQueryParams SERVICE_QUERY_PARAMS =
            ServiceQueryParamsImpl.builder()
                    .defaultFields(Sets.newHashSet("id", "mentorAccountId", "publicationUids", "deleted"))
//                    .addDefaultCriteriaItem("deleted", Collections.singletonList(false))
                    .defaultLimit(13)
                    .build();


    @Inject
    public AccountResource(AccountModel accountModel) throws RestDslException {
        super(accountModel, Account.class, ObjectId.class);
        this.accountModel = accountModel;
    }

    @GET
    @Path("/{id}/stats")
    public AccountStats getAccountStats(@PathParam("id") String id) {
        Account account = accountModel.getOne(getId(id));
        if (account == null) {
            throw new ServiceException("Account with id '" + id + "' not found", Response.Status.NOT_FOUND);
        }
        return account.getStats();
    }

    @Override
    public ServiceQueryParams getServiceQueryParams() {
        return SERVICE_QUERY_PARAMS;
    }

}
