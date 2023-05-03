package net.researchgate.restdsl.resources;

import com.mongodb.BasicDBObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import net.researchgate.restdsl.annotations.PATCH;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.model.ServiceModel;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.util.RequestUtil;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;


/**
 * Service resource that provides all HTTP verbs with default implementation.
 * If you don't want all of these, extend BaseServiceResource yourself and include what you want.
 */
public abstract class ServiceResource<V, K> extends BaseServiceResource<V, K> {
    private final ServiceModel<V, K> serviceModel;

    public ServiceResource(ServiceModel<V, K> serviceModel, Class<V> entityClazz, Class<K> idClazz) {
        super(serviceModel, entityClazz, idClazz);
        this.serviceModel = serviceModel;
    }

    @POST
    @Operation(summary = "Create a new  entity according to the business-logic")
    @ApiResponse(description = "Success case. Returns the created entity in the response body", responseCode = "201")
    @ApiResponse(description = "Client failure. Returns an error message response body", responseCode = "4xx")
    @ApiResponse(description = "Server failure. Returns an error message response body", responseCode = "5xx")
    public Response createEntity(V entity, @Context UriInfo uriInfo) throws RestDslException {
        validatePostEntity(entity);
        V persisted = serviceModel.save(entity);
        return Response.status(CREATED).entity(persisted).build();
    }

    @PATCH
    public V patchEntity(V entity, @Context UriInfo uriInfo) throws RestDslException {
        validatePatchEntity(entity);
        return serviceModel.patch(entity, RequestUtil.getPatchContext(uriInfo));
    }

    @Path(PATH_SEGMENT_PATTERN)
    @PUT
    public Response updateEntity(@PathParam("segment") String id, V entity, @Context UriInfo uriInfo) throws RestDslException {
        K key = getId(id);
        if (key == null) {
            throw new RestDslException("Key cannot be null", RestDslException.Type.PARAMS_ERROR);
        }
        validatePut(key, entity);

        entityInfo.setIdFieldValue(entity, key);
        V persisted = serviceModel.save(entity);
        return Response.status(OK).entity(persisted).build();
    }

    /*
    Deletes entities via a rest-dsl query.
    This is rather dangerous, clients may delete more than the intended entities.
    Consider extending BaseServiceResource instead, that contains a copy-paste-able example for deletion  by id.
     */
    @Operation(summary = "Use with caution! Deletes all entities, that matches the provided query. ", deprecated = true)
    @Path(PATH_SEGMENT_PATTERN)
    @DELETE
    @Produces("application/json;charset=UTF-8")
    public Response delete(@PathParam("segment") @Parameter(description = "A restDsl query. Ideally provide a single, explicit id. Otherwise ensure, that the query matches only the intended entities") PathSegment segment, @Context UriInfo uriInfo) throws RestDslException {
        ServiceQuery<K> query = getQueryFromRequest(segment, uriInfo);
        int deleted = serviceModel.delete(query);
        return Response.ok().entity(new BasicDBObject("deleted", deleted).toString()).build();
    }

    protected void validatePostEntity(V entity) throws RestDslException {
        // override if you need extra validation
    }

    protected void validatePatchEntity(V entity) throws RestDslException {
        K val = entityInfo.getIdFieldValue(entity);
        if (val == null) {
            throw new RestDslException("Id must be provided when patching an entity, but was null", RestDslException.Type.ENTITY_ERROR);
        }
    }

    protected void validatePut(K key, V entity) throws RestDslException {
        K val = entityInfo.getIdFieldValue(entity);
        if (val != null && !val.equals(key)) {
            throw new RestDslException("Id either should not be provided or be equal to the one in the entity, " +
                    "but was: " + val + " vs " + key, RestDslException.Type.ENTITY_ERROR);
        }
    }
}
