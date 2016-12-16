package net.researchgate.restler.service.resources;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.results.EntityResult;
import net.researchgate.restler.domain.Account;
import net.researchgate.restler.service.exceptions.ServiceException;
import net.researchgate.restler.service.model.AccountModel;
import org.bson.types.ObjectId;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Health resource and health check at the same time
 */
@Path("health")
@Singleton
@Produces("application/json;charset=UTF-8")
public class HealthResource extends HealthCheck {
    private final AccountModel accountModel;

    @Inject
    public HealthResource(AccountModel accountModel) {
        this.accountModel = accountModel;
    }

    @GET
    @Path("alive")
    public Response alive() throws Exception {
        Result res = check();
        if (res.isHealthy()) {
            return Response.ok(res).build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(res).build();
        }
    }

    @Override
    protected HealthCheck.Result check() throws Exception {
        ServiceQuery<ObjectId> build = ServiceQuery.<ObjectId>builder().limit(10).build();
        EntityResult<Account> accounts = accountModel.get(build);
        if (accounts.isEmpty()) {
            throw new ServiceException("No accounts found", Response.Status.INTERNAL_SERVER_ERROR);
        }
        String msg = "Healthy";
        return HealthCheck.Result.healthy(msg);
    }

}
