package net.researchgate.restdsl.dao;

import com.google.common.collect.Lists;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import net.researchgate.restdsl.TestEntity;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.metrics.NoOpStatsReporter;
import net.researchgate.restdsl.metrics.StatsReporter;
import net.researchgate.restdsl.queries.ServiceQuery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import static net.researchgate.restdsl.exceptions.RestDslException.Type.QUERY_ERROR;
import static org.junit.Assert.fail;

public class ServiceDaoTest {

    public static GenericContainer mongoDBContainer = new GenericContainer("mongo:4.4").withExposedPorts(27017);

    private Datastore fakedDatastore;
    private MongoClient client;

    @Before
    public void setUp() {
        mongoDBContainer.start();

        ConnectionString uri = new ConnectionString("mongodb://" + mongoDBContainer.getHost() + ":" + mongoDBContainer.getMappedPort(27017));
        System.setProperty("dw.mongoConfig.uri", uri.toString());
        System.setProperty("dw.mongoConfig.tls", "false");
        System.setProperty("dw.mongoConfig.dbName", "testDatabase");
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(uri)
                .build();


        client = MongoClients.create(clientSettings);
        fakedDatastore = Morphia.createDatastore(client, "testDatabase");
        final TestServiceDao dao = new TestServiceDao(fakedDatastore, TestEntity.class);

        // insert and remove an item in order to create indexes
        TestEntity dbEntity = dao.save(new TestEntity(1L, "test"));
        dao.delete(dbEntity.getId());
    }

    @After
    public void tearDown() throws Exception {
        // Clean up resources.
        mongoDBContainer.stop();
        client.close();
    }

    @Test
    public void testAllowGroupBy_doNotProvideAllowGroup_allow() {
        final TestServiceDao dao = new TestServiceDao(fakedDatastore, TestEntity.class);

        Assert.assertTrue(dao.allowGroupBy);
        final ServiceQuery<Long> q = ServiceQuery.<Long>builder()
                .withCriteria("id", Lists.newArrayList(1L, 2L, 3L))
                .groupBy("id")
                .build();
        dao.get(q);
    }

    @Test
    public void testAllowGroupBy_explicitlyDisallowGroupBy_doNotAllowQueryWithGroupBy() {
        final TestServiceDao dao = new TestServiceDao(fakedDatastore, TestEntity.class, NoOpStatsReporter.INSTANCE, false);
        Assert.assertFalse(dao.allowGroupBy);

        final ServiceQuery<Long> q = ServiceQuery.<Long>builder()
                .withCriteria("id", Lists.newArrayList(1L, 2L, 3L))
                .groupBy("id")
                .build();
        // get with group by -> fail
        try {
            dao.get(q);
            fail("Group by should not be allowed!, but it was");
        } catch (RestDslException e) {
            Assert.assertEquals(QUERY_ERROR, e.getType());
        }

        // get without groupBy -> successful
        dao.get(ServiceQuery.byId(1L));

        // explicitly allow group by -> successful
        dao.setAllowGroupBy();
        Assert.assertTrue(dao.allowGroupBy);
        dao.get(q);
    }

    static class TestServiceDao extends MongoServiceDao<TestEntity, Long> {
        public TestServiceDao(Datastore datastore, Class<TestEntity> entityClazz) {
            super(datastore, entityClazz);
        }

        public TestServiceDao(Datastore datastore, Class<TestEntity> entityClazz, StatsReporter statsReporter, boolean allowGroupBy) {
            super(datastore, entityClazz, statsReporter, allowGroupBy);
        }
    }

}
