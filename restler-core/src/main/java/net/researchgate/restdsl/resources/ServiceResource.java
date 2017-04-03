package net.researchgate.restdsl.resources;

import com.mongodb.BasicDBObject;
import net.researchgate.restdsl.annotations.PATCH;
import net.researchgate.restdsl.domain.EntityInfo;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.model.ServiceModel;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.queries.ServiceQueryInfo;
import net.researchgate.restdsl.queries.ServiceQueryParams;
import net.researchgate.restdsl.results.EntityResult;
import net.researchgate.restdsl.types.TypeInfoUtil;
import net.researchgate.restdsl.util.RequestUtil;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.lang.reflect.ParameterizedType;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;


/**
 * Commons methods for CRUD
 * V - entity type
 * K - entity primary key type
 */
public abstract class ServiceResource<V, K> {
    private final EntityInfo<V> entityInfo;
    private final Class<V> entityClazz;
    private final Class<K> idClazz;
    private final ServiceModel<V, K> serviceModel;

    public ServiceResource(ServiceModel<V, K> serviceModel, Class<V> entityClazz, Class<K> idClazz) {
        this.serviceModel = serviceModel;
        this.entityClazz = entityClazz;
        this.idClazz = idClazz;
        entityInfo = EntityInfo.get(entityClazz);
    }

    @SuppressWarnings("unchecked")
    public ServiceResource(ServiceModel<V, K> serviceModel) throws RestDslException {
        Class<? extends ServiceModel> serviceModelClazz = serviceModel.getClass();
        if (serviceModelClazz == ServiceModel.class || serviceModelClazz.getSuperclass() != ServiceModel.class) {
            throw new RestDslException("Unable to detect entity and key type from class " + serviceModelClazz.getName() +
                    "; use constructor with explicit entity and key classes",
                    RestDslException.Type.GENERAL_ERROR);
        }

        ParameterizedType t = (ParameterizedType) serviceModelClazz.getAnnotatedSuperclass().getType();
        this.entityClazz = (Class<V>) t.getActualTypeArguments()[0];
        this.idClazz = (Class<K>) t.getActualTypeArguments()[1];
        entityInfo = EntityInfo.get(entityClazz);
        this.serviceModel = serviceModel;
    }

    @Path("/{segment: .*}")
    @GET
    @Produces("application/json")
    public EntityResult<V> getEntityResult(@PathParam("segment") PathSegment segment, @Context UriInfo uriInfo) throws RestDslException {
        ServiceQuery<K> query = getQueryFromRequest(segment, uriInfo);
        return serviceModel.get(query);
    }

    @Path("/{segment: .*}/info")
    @GET
    @Produces("application/json")
    public ServiceQueryInfo<K> getQueryInfo(@PathParam("segment") PathSegment segment, @Context UriInfo uriInfo) throws RestDslException {
        ServiceQuery<K> query = getQueryFromRequest(segment, uriInfo);
        return serviceModel.getServiceQueryInfo(query);
    }

    @PATCH
    public V patchEntity(V entity, @Context UriInfo uriInfo) throws RestDslException {
        validatePatchEntity(entity);
        return serviceModel.patch(entity, RequestUtil.getPatchContext(uriInfo));
    }

    @POST
    public Response createEntity(V entity) throws RestDslException {
        validatePostEntity(entity);
        V persisted = serviceModel.save(entity);
        return Response.status(CREATED).entity(persisted).build();
    }

    @PUT
    @Path("/{id: .*}")
    public Response updateEntity(@PathParam("id") String id, V entity) throws RestDslException {
        K key = getId(id);
        if (key == null) {
            throw new RestDslException("Key cannot be null", RestDslException.Type.PARAMS_ERROR);
        }
        validatePut(key, entity);

        entityInfo.setIdFieldValue(entity, key);
        V persisted = serviceModel.save(entity);
        return Response.status(OK).entity(persisted).build();
    }

    protected K getId(String id) throws RestDslException {
        return TypeInfoUtil.getValue(id, EntityInfo.get(entityClazz).getIdFieldName(), idClazz, entityClazz);
    }

    @Path("/{segment: .*}")
    @DELETE
    @Produces("application/json")
    public Response delete(@PathParam("segment") PathSegment segment, @Context UriInfo uriInfo) throws RestDslException {
        ServiceQuery<K> query = getQueryFromRequest(segment, uriInfo);
        int deleted = serviceModel.delete(query);
        return Response.ok().entity(new BasicDBObject("deleted", deleted).toString()).build();
    }


    protected ServiceQuery<K> getQueryFromRequest(PathSegment segment, UriInfo uriInfo) throws RestDslException {
        return RequestUtil.parseCommonParameters(entityClazz, idClazz, segment, uriInfo, getServiceQueryParams())
                .build();
    }

    protected ServiceQuery.ServiceQueryBuilder<K> getBuilderFromRequest(PathSegment segment, UriInfo uriInfo) throws RestDslException {
        return RequestUtil.parseCommonParameters(entityClazz, idClazz, segment, uriInfo, getServiceQueryParams());
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

    protected ServiceQueryParams getServiceQueryParams() {
        return ServiceQueryParams.DEFAULT_QUERY_PARAMS;
    }
}
