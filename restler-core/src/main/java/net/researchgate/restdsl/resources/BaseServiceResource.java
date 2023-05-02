package net.researchgate.restdsl.resources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * 3. Call super.createEntity() in it, or custom logic.
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

    @Operation(summary = "Retrieve entities using the generic researchgate 'restler' query-language: https://github.com/researchgate/restler#get")
    @Parameter(name = "segment", in = ParameterIn.PATH, schema = @Schema(type = "string", example = "-"), description = "A rest-dsl query, See https://github.com/researchgate/restler#get")
    @Parameter(name = "fields", in = ParameterIn.QUERY, description = "Only return this list of comma-separated fields. Use '*' to return all", example = "*")
    @Parameter(name = "limit", in = ParameterIn.QUERY, schema = @Schema(type = "integer", format = "int32"), description = "Limit the number of returned records")
    @Parameter(name = "offset", in = ParameterIn.QUERY, schema = @Schema(type = "integer", format = "int32"), description = "Skip this many records. Use this together with limit to implement pagination.")
    @Parameter(name = "order", in = ParameterIn.QUERY, schema = @Schema(type = "string"), description = "Sort the result by this field in ascending order. The field can be prefixed with '-' to sort in descending order")
    @Parameter(name = "countTotalItems", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "whether to count the total items. Setting this to 'false' will remove the 'list.totalItems' property and may improve response times (true by default)")
    @Parameter(name = "groupBy", in = ParameterIn.QUERY, description = "Group by a certain field. use with caution. groupBy can be forbidden by the dao, in order to prevent too much load on the database.")
    @Parameter(name = "indexValidation", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "Use with caution during development! Setting this to true disables the safeguard of ensuring, that the request is can effeciently be supported by the database, meaning a usable index exists. (true by default)")
    @Path(PATH_SEGMENT_PATTERN)
    @GET
    public EntityResult<V> getEntityResult(@PathParam("segment") PathSegment segment, @Context UriInfo uriInfo) throws RestDslException {
        ServiceQuery<K> query = getQueryFromRequest(segment, uriInfo);
        return serviceModel.get(query);
    }

    @Operation(summary = "Returns a human readable description of the generated database operations. This is intended for client to develop and debug rest-dsl queries")
    @Parameter(name = "segment", in = ParameterIn.PATH, schema = @Schema(type = "string", example = "-"), description = "A rest-dsl query, See https://github.com/researchgate/restler#get")
    @Parameter(name = "fields", in = ParameterIn.QUERY, description = "Only return this list of comma-separated fields. Use '*' to return all", example = "*")
    @Parameter(name = "limit", in = ParameterIn.QUERY, schema = @Schema(type = "integer", format = "int32"), description = "Limit the number of returned records")
    @Parameter(name = "offset", in = ParameterIn.QUERY, schema = @Schema(type = "integer", format = "int32"), description = "Skip this many records. Use this together with limit to implement pagination.")
    @Parameter(name = "order", in = ParameterIn.QUERY, schema = @Schema(type = "string"), description = "Sort the result by this field in ascending order. The field can be prefixed with '-' to sort in descending order")
    @Parameter(name = "countTotalItems", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "whether to count the total items. Setting this to 'false' will remove the 'list.totalItems' property and may improve response times (true by default)")
    @Parameter(name = "groupBy", in = ParameterIn.QUERY, description = "Group by a certain field. use with caution. groupBy can be forbidden by the dao, in order to prevent too much load on the database.")
    @Parameter(name = "indexValidation", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "Use with caution during development! Setting thisto true disables the safeguard of ensuring, that the request is can effeciently be supported by the database, meaning a usable index exists. (true by default)")
    @Path(PATH_SEGMENT_PATTERN + "/info")
    @GET
    public ServiceQueryInfo<K> getQueryInfo(@PathParam("segment") PathSegment segment, @Context UriInfo uriInfo) throws RestDslException {
        ServiceQuery<K> query = getQueryFromRequest(segment, uriInfo);
        return serviceModel.getServiceQueryInfo(query);
    }

// This method is intentionally commented out, in order to not to expose deletes to clients by default.
// The following stub is a starting point to copyPaste into your subclass
//
//    /**
//     * Delete the entity by its id.
//     *
//     * @return 200, if the entity was deleted, 404 otherwise
//     */
//    @Path("/{id}")
//    @DELETE
//    @Produces("application/json;charset=UTF-8")
//    public Response delete(@PathParam("id") @Schema(description = "id of the entity to delete") String idString) throws RestDslException {
//        final K id = TypeInfoUtil.getValue(idString, null, idClazz, null);
//        int deleted = serviceModel.delete(id);
//        if (deleted == 0) {
//            throw new NotFoundException();
//        }
//        return Response.ok().build();
//    }

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
