package net.researchgate.restler.service.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.inject.Inject;
import net.researchgate.restdsl.exceptions.RestDslException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Provider
public class ServiceExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Inject
    public ServiceExceptionMapper() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof ServiceException && ((ServiceException) exception).getStatus() == Status.NOT_MODIFIED) {
            return Response.notModified().build();
        }
        String requestUri = uriInfo.getRequestUri().toString();

        Status status = getStatus(exception);

        String exceptionName = getExceptionName(exception);

        List<String> descriptions = new ArrayList<>();

        Throwable currentException = exception;
        int maxIterations = 0;
        while (currentException != null) {
            StringBuilder sb = new StringBuilder(50);
            sb.append(currentException.getClass().getName());
            if (currentException.getMessage() != null) {
                sb.append(":").append(currentException.getMessage());
            }
            descriptions.add(sb.toString());
            currentException = currentException.getCause();
            maxIterations++;
            if (maxIterations > 4) {
                break;
            }
        }

        if (status.getFamily() == Status.Family.SERVER_ERROR) {
            LOGGER.error("Exception Not Mapped , Server Error", exception);
        } else if (status == Status.NOT_FOUND) {
            LOGGER.debug("Exception Mapped {}", descriptions);
        } else {
            LOGGER.info("Exception Mapped {}", descriptions);
        }

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(buildJson(exceptionName, descriptions, status, requestUri))
                .build();
    }

    private Status getStatus(Throwable exception) {
        Status status = Status.INTERNAL_SERVER_ERROR;
        if (exception instanceof ServiceException && ((ServiceException) exception).getStatus() != null) {
            status = Status.fromStatusCode(((ServiceException) exception).getStatus().getStatusCode());
        } else if (exception instanceof RestDslException) {
            return restDslTypeToResponseCode(((RestDslException) exception).getType());
        }
        return status;
    }

    private Status restDslTypeToResponseCode(RestDslException.Type type) {
        if (type == RestDslException.Type.GENERAL_ERROR) {
            return Status.INTERNAL_SERVER_ERROR;
        } else if (type == RestDslException.Type.PARAMS_ERROR) {
            return Status.BAD_REQUEST;
        } else if (type == RestDslException.Type.ENTITY_ERROR) {
            return Status.BAD_REQUEST;
        } else if (type == RestDslException.Type.DUPLICATE_KEY) {
            return Status.CONFLICT;
        } else if (type == RestDslException.Type.QUERY_ERROR) {
            return Status.BAD_REQUEST;
        } else {
            return Status.INTERNAL_SERVER_ERROR;
        }
    }

    private String getExceptionName(Throwable exception) {
        String exceptionName = exception.getClass().getCanonicalName();
        if (exception.getClass().equals(RuntimeException.class) &&
                exception.getCause() != null) {
            exceptionName = exception.getCause().getClass().getCanonicalName();
        }
        return exceptionName;
    }


    public static String buildJson(String type, List<String> descriptions, Status status, String requestUri) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("type", type);
        root.put("description", Joiner.on("\n").join(descriptions));
        root.put("code", status.getStatusCode());
        root.put("requestUri", requestUri);
        String json;
        try {
            json = mapper.writeValueAsString(root);
        } catch (IOException e) {
            LOGGER.warn("Error in ServiceExceptionMapper: ", e);
            json = "{\"description\": \"Internal error: " + e.getMessage() + "\"}";
        }
        return json;
    }


}
