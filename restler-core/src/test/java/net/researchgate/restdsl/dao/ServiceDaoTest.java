package net.researchgate.restdsl.dao;

import com.github.fakemongo.Fongo;
import com.google.common.collect.Lists;
import net.researchgate.restdsl.TestEntity;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.metrics.NoOpStatsReporter;
import net.researchgate.restdsl.metrics.StatsReporter;
import net.researchgate.restdsl.queries.ServiceQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import static net.researchgate.restdsl.exceptions.RestDslException.Type.QUERY_ERROR;
import static org.junit.Assert.fail;

public class ServiceDaoTest {

    // This merely allow us to spin up a a dao.
    private Datastore fakedDatastore;

    @Before
    public void setUp() {
        Fongo fongo = new Fongo("fake mongo");
        fakedDatastore = new Morphia().createDatastore(fongo.getMongo(), "testDatabase");
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
