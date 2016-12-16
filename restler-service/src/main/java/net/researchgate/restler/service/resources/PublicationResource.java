package net.researchgate.restler.service.resources;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.resources.ServiceResource;
import net.researchgate.restler.domain.Publication;
import net.researchgate.restler.service.model.PublicationModel;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * Publication resource
 *
 */
@Path("publications")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class PublicationResource extends ServiceResource<Publication, Long> {
    private PublicationModel publicationModel;

    @Inject
    public PublicationResource(PublicationModel publicationModel) throws RestDslException {
        super(publicationModel, Publication.class, Long.class);
        this.publicationModel = publicationModel;
    }

    @Path("/external/{id}")
    @GET
    @Produces("application/json")
    public Publication getExternalPublication(@PathParam("id") Long id, @Context UriInfo uriInfo) throws RestDslException {
        return publicationModel.getExternalPublication(id);
    }
}
