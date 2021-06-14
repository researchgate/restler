package net.researchgate.restdsl.resources;

import net.researchgate.restdsl.domain.EntityInfo;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.model.BaseServiceModel;
import net.researchgate.restdsl.model.ServiceModel;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.queries.ServiceQueryInfo;
import net.researchgate.restdsl.queries.ServiceQueryParams;
import net.researchgate.restdsl.results.EntityResult;
import net.researchgate.restdsl.types.TypeInfoUtil;
import net.researchgate.restdsl.util.RequestUtil;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import java.lang.reflect.ParameterizedType;

/**
 * Commons methods for CRUD. This base class includes only the HTTP GET verb. If you want the others too,
 * 1. Extend this class
 * 2. Override the corresponding method e.g. createEntity().
 * 3. Just call super.createEntity() in it, or custom logic.
 * 4. Annotate the overridden method with e.g. @POST or @PATH(...) where applicable.
 * <p>
 * V - entity type
 * K - entity primary key type
 */
public abstract class BaseServiceResource<V, K> {
    private final Class<V> entityClazz;
    private final Class<K> idClazz;

    protected final EntityInfo<V> entityInfo;
    private final BaseServiceModel<V, K> serviceModel;

    /* Matches "/" followed by as little as possible to the end or the next non-encoded slash. */
    public static final String PATH_SEGMENT_PATTERN = "/{segment: [^/]*?}";

    public BaseServiceResource(BaseServiceModel<V, K> serviceModel, Class<V> entityClazz, Class<K> idClazz) {
        this.serviceModel = serviceModel;
        this.entityClazz = entityClazz;
        this.idClazz = idClazz;
        entityInfo = EntityInfo.get(entityClazz);
    }

    @SuppressWarnings("unchecked")
    public BaseServiceResource(BaseServiceModel<V, K> serviceModel) throws RestDslException {

        if (serviceModel.getClass().getSuperclass() != ServiceModel.class && serviceModel.getClass().getSuperclass() != BaseServiceModel.class) {
            throw new RestDslException("Unable to detect entity and key type from class " + serviceModel.getClass().getName() +
                    "; use constructor with explicit entity and key classes",
                    RestDslException.Type.GENERAL_ERROR);
        }

        // TODO: perhaps its better to expose the EntityInfo in ServiceDao & serviceModel?
        ParameterizedType t = (ParameterizedType) serviceModel.getClass().getAnnotatedSuperclass().getType();
        this.entityClazz = (Class<V>) t.getActualTypeArguments()[0];
        this.idClazz = (Class<K>) t.getActualTypeArguments()[1];
        entityInfo = EntityInfo.get(entityClazz);
        this.serviceModel = serviceModel;
    }

    @Path(PATH_SEGMENT_PATTERN)
    @GET
    public EntityResult<V> getEntityResult(@PathParam("segment") PathSegment segment, @Context UriInfo uriInfo) throws RestDslException {
        ServiceQuery<K> query = getQueryFromRequest(segment, uriInfo);
        return serviceModel.get(query);
    }

    @Path(PATH_SEGMENT_PATTERN + "/info")
    @GET
    public ServiceQueryInfo<K> getQueryInfo(@PathParam("segment") PathSegment segment, @Context UriInfo uriInfo) throws RestDslException {
        ServiceQuery<K> query = getQueryFromRequest(segment, uriInfo);
        return serviceModel.getServiceQueryInfo(query);
    }

    protected K getId(String id) throws RestDslException {
        return TypeInfoUtil.getValue(id, EntityInfo.get(entityClazz).getIdFieldName(), idClazz, entityClazz);
    }

    protected ServiceQuery<K> getQueryFromRequest(PathSegment segment, UriInfo uriInfo) throws RestDslException {
        return RequestUtil.parseRequest(entityClazz, idClazz, segment, uriInfo, getServiceQueryParams());
    }


    protected ServiceQueryParams getServiceQueryParams() {
        return ServiceQueryParams.DEFAULT_QUERY_PARAMS;
    }
}
