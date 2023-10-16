package net.researchgate.restdsl.dao;

import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.results.EntityResult;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

/**
 * Calls external service to get data
 */
public abstract class RestApiServiceDao<V, K> implements ServiceDao<V, K> {
    private String baseUrl;
    private Class<V> entityClazz;
    private final Client client;

    public RestApiServiceDao(String baseUrl, Class<V> entityClazz) {
        this.baseUrl = baseUrl;
        this.entityClazz = entityClazz;
        client = ClientBuilder.newClient();
    }

    @Override
    public EntityFieldMapper getEntityMapper() {
        return new GenericFieldMapper();
    }

    @Override
    public EntityResult<V> get(ServiceQuery<K> serviceQuery) throws RestDslException {
        Invocation.Builder request = client.target(baseUrl + serviceQuery.toUrlPart()).request();

        Response response = request.get();

        EntityResult<V> result;
        try {
            result = response.readEntity(EntityResult.getGenericType(entityClazz));
        } catch (Exception e) {
            throw new RestDslException("Cannot fetch entities for query " + serviceQuery, RestDslException.Type.GENERAL_ERROR);
        }
        return result;
    }

    @Override
    public V getOne(ServiceQuery<K> serviceQuery) throws RestDslException {
        EntityResult<V> collection = get(serviceQuery);

        if (collection.isEmpty()) {
            return null;
        }
        return collection.iterator().next();
    }

    @Override
    public long count(ServiceQuery<K> serviceQuery) throws RestDslException {
        return get(serviceQuery).getTotalItems();
    }

    @Override
    public void validateQuery(ServiceQuery<K> query) throws RestDslException {
        //no op --
    }
}
