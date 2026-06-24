package net.researchgate.restler.service.resources;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import net.researchgate.restler.RestlerApplication;
import net.researchgate.restler.domain.Publication;
import net.researchgate.restler.service.config.RestlerConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


/**
 * Testing publication resource
 */
public class PublicationResourceTest {

    public static final DropwizardTestSupport<RestlerConfig> APP =
            new DropwizardTestSupport<>(RestlerApplication.class, ResourceHelpers.resourceFilePath("config.yaml"));

    @BeforeClass
    public static void setUp() throws Exception {
        APP.before();
    }

    @AfterClass
    public static void tearDown() {
        APP.after();
    }

    //TODO: make sure headers are propagated
    @Test
    @Ignore
    public void testBasic() {
        String clientName = "community";
        Client client = new JerseyClientBuilder().build();

        Publication p = new Publication();
        p.setPublicationUid(1L);
        p.setTitle("Test title");

        String url = String.format("http://localhost:%d%s/publications", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());
        Response response = client.target(url)
                .request()
                .header("X-Client-Id", clientName)
                .post(Entity.entity(p, MediaType.APPLICATION_JSON_TYPE));

        Publication p2 = response.readEntity(Publication.class);
        assertNotEquals(p, p2);

        assertEquals(clientName, p2.getLastModificationClientId());
    }

    @Test
    @Ignore // Ignoring since it requires a locally running service
    public void testExternalDao() {
        Client client = new JerseyClientBuilder().build();

        Publication p = new Publication();
        p.setPublicationUid(1L);
        p.setTitle("Test title");

        String url = String.format("http://localhost:%d%s/publications", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());
        client.target(url)
                .request()
                .post(Entity.entity(p, MediaType.APPLICATION_JSON_TYPE));

        url = String.format("http://localhost:%d%s/publications/external/1", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());

        Publication p2 = client.target(url).request().get(Publication.class);

        assertEquals(p, p2);
    }

}
