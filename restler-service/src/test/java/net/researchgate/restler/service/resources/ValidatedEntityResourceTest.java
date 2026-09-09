package net.researchgate.restler.service.resources;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import net.researchgate.restler.RestlerApplication;
import net.researchgate.restler.domain.ValidatedEntity;
import net.researchgate.restler.service.config.RestlerConfig;
import net.researchgate.restler.service.util.MongoDContainerRule;
import org.bson.types.ObjectId;
import org.glassfish.jersey.apache5.connector.Apache5ConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * Testing validated entity resource
 */
public class ValidatedEntityResourceTest {
    private static final MongoDContainerRule MONGODB = new MongoDContainerRule("testDb");

    private static final DropwizardTestSupport<RestlerConfig> APP =
            new DropwizardTestSupport<>(RestlerApplication.class, ResourceHelpers.resourceFilePath("config.yaml"));

    private final Client client = new JerseyClientBuilder()
            .withConfig(new ClientConfig().connectorProvider(new Apache5ConnectorProvider()))
            .build();

    @BeforeClass
    public static void setUp() throws Exception {
        MONGODB.start();
        APP.before();
    }

    @AfterClass
    public static void afterClass() {
        APP.after();
        MONGODB.stop();
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void testPut_allNotNullAndValid_succeeds() {
        ObjectId id = new ObjectId();
        ValidatedEntity entity = new ValidatedEntity(id, "not null", 1);

        String url = String.format("http://localhost:%d%s/validated-entities/{id}", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());
        try (Response response = client.target(url)
                .resolveTemplate("id", id.toHexString())
                .request()
                .put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE))) {

            assertTrue(200 <= response.getStatus() && response.getStatus() < 300);

            ValidatedEntity result = response.readEntity(ValidatedEntity.class);

            assertEquals(entity, result);
        }
    }

    @Test
    public void testPut_validWithNullPositiveValue_succeeds() {
        ObjectId id = new ObjectId();
        ValidatedEntity entity = new ValidatedEntity(id, "not null", null);

        String url = String.format("http://localhost:%d%s/validated-entities/{id}", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());
        try (Response response = client.target(url)
                .resolveTemplate("id", id.toHexString())
                .request()
                .put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE))) {

            assertTrue(200 <= response.getStatus() && response.getStatus() < 300);

            ValidatedEntity result = response.readEntity(ValidatedEntity.class);

            assertEquals(entity, result);
        }
    }

    @Test
    public void testPut_notNullFieldIsNull_fails() {
        ObjectId id = new ObjectId();
        ValidatedEntity entity = new ValidatedEntity(id, null, null);

        String url = String.format("http://localhost:%d%s/validated-entities/{id}", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());

        try (Response response = client.target(url)
                .resolveTemplate("id", id.toHexString())
                .request()
                .put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE))) {
            assertTrue(400 <= response.getStatus() && response.getStatus() < 500);
        }
    }

    @Test
    public void testPut_positiveFieldIsNegative_fails() {
        ObjectId id = new ObjectId();
        ValidatedEntity entity = new ValidatedEntity(id, "not null", -1);

        String url = String.format("http://localhost:%d%s/validated-entities/{id}", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());

        try (Response response = client.target(url)
                .resolveTemplate("id", id.toHexString())
                .request()
                .put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE))) {
            assertTrue(400 <= response.getStatus() && response.getStatus() < 500);
        }
    }

    @Test
    public void testPost_allNotNullAndValid_succeeds() {
        ValidatedEntity entity = new ValidatedEntity(null, "not null", 1);

        String url = String.format("http://localhost:%d%s/validated-entities", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());
        try (Response response = client.target(url)
                .request()
                .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE))) {

            assertTrue(200 <= response.getStatus() && response.getStatus() < 300);

            ValidatedEntity result = response.readEntity(ValidatedEntity.class);

            result.setId(null);
            assertEquals(entity, result);
        }
    }

    @Test
    public void testPost_validWithNullPositiveValue_succeeds() {
        ValidatedEntity entity = new ValidatedEntity(null, "not null", null);

        String url = String.format("http://localhost:%d%s/validated-entities", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());
        try (Response response = client.target(url)
                .request()
                .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE))) {

            assertTrue(200 <= response.getStatus() && response.getStatus() < 300);

            ValidatedEntity result = response.readEntity(ValidatedEntity.class);

            result.setId(null);
            assertEquals(entity, result);
        }
    }

    @Test
    public void testPost_notNullFieldIsNull_fails() {
        ValidatedEntity entity = new ValidatedEntity(null, null, null);

        String url = String.format("http://localhost:%d%s/validated-entities", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());

        try (Response response = client.target(url)
                .request()
                .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE))) {
            assertTrue(400 <= response.getStatus() && response.getStatus() < 500);
        }
    }

    @Test
    public void testPost_positiveFieldIsNegative_fails() {
        ValidatedEntity entity = new ValidatedEntity(null, "not null", -1);

        String url = String.format("http://localhost:%d%s/validated-entities", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());

        try (Response response = client.target(url)
                .request()
                .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE))) {
            assertTrue(400 <= response.getStatus() && response.getStatus() < 500);
        }
    }

    @Test
    public void testPatch_nothingToPatch_succeeds() {
        ValidatedEntity entity = putEntity();

        String url = String.format("http://localhost:%d%s/validated-entities", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());

        try (Response response = client.target(url)
                .request()
                .build("PATCH", Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE))
                .invoke()) {
            assertTrue(200 <= response.getStatus() && response.getStatus() < 300);

            ValidatedEntity result = response.readEntity(ValidatedEntity.class);

            assertEquals(entity, result);
        }
    }

    @Test
    public void testPatch_patchNotNullToNotNull_succeeds() {
        ValidatedEntity entity = putEntity();

        String url = String.format("http://localhost:%d%s/validated-entities", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());

        ValidatedEntity patchEntity = new ValidatedEntity(
                entity.getId(),
                "patched " + entity.getNotNullField(),
                null
        );

        try (Response response = client.target(url)
                .request()
                .build("PATCH", Entity.entity(patchEntity, MediaType.APPLICATION_JSON_TYPE))
                .invoke()) {
            assertTrue(200 <= response.getStatus() && response.getStatus() < 300);

            ValidatedEntity result = response.readEntity(ValidatedEntity.class);

            assertEquals(patchEntity.getNotNullField(), result.getNotNullField());
            assertEquals(entity.getPositiveField(), result.getPositiveField());
        }
    }

    @Test
    public void testPatch_patchPositiveToPositive_succeeds() {
        ValidatedEntity entity = putEntity();

        String url = String.format("http://localhost:%d%s/validated-entities", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());

        ValidatedEntity patchEntity = new ValidatedEntity(
                entity.getId(),
                null,
                entity.getPositiveField() != null ? entity.getPositiveField() + 1 : 1
        );

        try (Response response = client.target(url)
                .request()
                .build("PATCH", Entity.entity(patchEntity, MediaType.APPLICATION_JSON_TYPE))
                .invoke()) {
            assertTrue(200 <= response.getStatus() && response.getStatus() < 300);

            ValidatedEntity result = response.readEntity(ValidatedEntity.class);

            assertEquals(entity.getNotNullField(), result.getNotNullField());
            assertEquals(patchEntity.getPositiveField(), result.getPositiveField());
        }
    }

    @Test
    public void testPatch_unsetPositive_succeeds() {
        ValidatedEntity entity = putEntity();

        String url = String.format("http://localhost:%d%s/validated-entities", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());

        ValidatedEntity patchEntity = new ValidatedEntity(
                entity.getId(),
                null,
                null
        );

        try (Response response = client.target(url)
                .queryParam("unsetFields", "positiveField")
                .request()
                .build("PATCH", Entity.entity(patchEntity, MediaType.APPLICATION_JSON_TYPE))
                .invoke()) {
            assertTrue(200 <= response.getStatus() && response.getStatus() < 300);

            ValidatedEntity result = response.readEntity(ValidatedEntity.class);

            assertEquals(entity.getNotNullField(), result.getNotNullField());
            assertNull(result.getPositiveField());
        }
    }

    @Test
    public void testPatch_unsetNotNullField_fails() {
        ValidatedEntity entity = putEntity();

        ValidatedEntity patchEntity = new ValidatedEntity(
                entity.getId(),
                null,
                null
        );

        String url = String.format("http://localhost:%d%s/validated-entities", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());

        try (Response response = client.target(url)
                .queryParam("unsetFields", "notNullField")
                .request()
                .build("PATCH", Entity.entity(patchEntity, MediaType.APPLICATION_JSON_TYPE))
                .invoke()) {
            assertTrue(400 <= response.getStatus() && response.getStatus() < 500);
        }
    }

    @Test
    public void testPatch_patchNegative_fails() {
        ValidatedEntity entity = putEntity();

        ValidatedEntity patchEntity = new ValidatedEntity(
                entity.getId(),
                null,
                -1
        );

        String url = String.format("http://localhost:%d%s/validated-entities", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());

        try (Response response = client.target(url)
                .request()
                .build("PATCH", Entity.entity(patchEntity, MediaType.APPLICATION_JSON_TYPE))
                .invoke()) {
            assertTrue(400 <= response.getStatus() && response.getStatus() < 500);
        }
    }

    private ValidatedEntity putEntity() {
        ObjectId id = new ObjectId();
        ValidatedEntity entity = new ValidatedEntity(id, "not null", 1);

        String url = String.format("http://localhost:%d%s/validated-entities/{id}", APP.getLocalPort(),
                APP.getEnvironment().getApplicationContext().getContextPath());
        try (Response response = client.target(url)
                .resolveTemplate("id", id.toHexString())
                .request()
                .put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE))) {

            return response.readEntity(ValidatedEntity.class);
        }
    }
}
