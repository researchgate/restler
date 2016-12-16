package net.researchgate.restler.service.exceptions;

import javax.ws.rs.core.Response;


public class ServiceException extends RuntimeException {
    private static final long serialVersionUID = 3403608589330157070L;

    private Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(Throwable e) {
        super(e);
    }

    public ServiceException(String message, Throwable e) {
        super(message, e);
    }

    public ServiceException(String message, Response.Status status) {
        super(message);
        this.status = status;
    }

    public ServiceException(String message, Throwable cause, Response.Status status) {
        super(message, cause);
        this.status = status;
    }


    public Response.Status getStatus() {
        return status;
    }
}
