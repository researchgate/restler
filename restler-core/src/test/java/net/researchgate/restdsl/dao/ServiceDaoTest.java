package net.researchgate.restdsl.dao;

import com.google.common.collect.Lists;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.IndexOptions;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import net.researchgate.restdsl.GroupByEntity;
import net.researchgate.restdsl.TestEntity;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.metrics.MetricSink;
import net.researchgate.restdsl.metrics.NoOpMetricSink;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.results.EntityList;
import net.researchgate.restdsl.results.EntityMultimap;
import net.researchgate.restdsl.results.EntityResult;
import org.bson.Document;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.smallrye.common.constraint.Assert.assertNotNull;
import static java.lang.String.format;
import static net.researchgate.restdsl.exceptions.RestDslException.Type.QUERY_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

public class ServiceDaoTest {

    public static GenericContainer mongoDBContainer = new GenericContainer("mongo:4.4").withExposedPorts(27017);

    private Datastore fakedDatastore;
    private MongoClient client;

    @Before
    public void setUp() {
        mongoDBContainer.start();

        ConnectionString uri = new ConnectionString("mongodb://" + mongoDBContainer.getHost() + ":" + mongoDBContainer.getMappedPort(27017));
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(uri)
                .build();

        client = MongoClients.create(clientSettings);
        fakedDatastore = Morphia.createDatastore(client, "testDatabase");
        final TestServiceDao dao = new TestServiceDao(fakedDatastore, TestEntity.class);

        // insert and remove an item in order to create indexes
        TestEntity dbEntity = dao.save(new TestEntity(1L, "test"));
        dao.delete(dbEntity.getId());

        fakedDatastore.getCollection(GroupByEntity.class).createIndex(Document.parse("{ group: 1 }"));
        final TestWithDateDao daoWithDate = new TestWithDateDao(fakedDatastore);
        final Instant start = LocalDate.of(2025, 1, 1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        for (int group = 1; group < 20; group++) {
            for (int days = 0; days < group; days++) {
                final Date date = Date.from(start.plus(Period.ofDays(days)));
                final String groupString = format("test%02d", group);
                final GroupByEntity entity = new GroupByEntity(null, groupString, date);
                daoWithDate.save(entity);
            }
        }
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
    public void testGroupBy_equals() {
        final TestWithDateDao dao = new TestWithDateDao(fakedDatastore);

        Assert.assertTrue(dao.allowGroupBy);

        final List<String> expectedGroups = List.of("test02", "test03", "test04", "DOES_NOT_EXIST");
        final ServiceQuery<Long> q = ServiceQuery.<Long>builder()
                .withCriteria("group", expectedGroups)
                .order("-date")
                .groupBy("group")
                .limit(3)
                .build();
        EntityResult<GroupByEntity> result = dao.get(q);
        EntityMultimap<GroupByEntity> multimap = result.getMultimap();

        assertEquals(Set.copyOf(expectedGroups), multimap.getItems().keySet());

        EntityList<GroupByEntity> group02 = multimap.getItems().get("test02");
        assertNotNull(group02);
        assertEquals(Long.valueOf(2L), group02.getTotalItems());
        assertEquals(
                List.of(
                        utcDate(2025, 1, 2),
                        utcDate(2025, 1, 1)
                ),
                getDates(group02)
        );

        EntityList<GroupByEntity> group03 = multimap.getItems().get("test03");
        assertNotNull(group03);
        assertEquals(Long.valueOf(3L), group03.getTotalItems());
        assertEquals(
                List.of(
                        utcDate(2025, 1, 3),
                        utcDate(2025, 1, 2),
                        utcDate(2025, 1, 1)
                ),
                getDates(group03)
        );

        EntityList<GroupByEntity> group04 = multimap.getItems().get("test04");
        assertNotNull(group04);
        assertEquals(Long.valueOf(4L), group04.getTotalItems());
        assertEquals(
                List.of(
                        utcDate(2025, 1, 4),
                        utcDate(2025, 1, 3),
                        utcDate(2025, 1, 2)
                ),
                getDates(group04)
        );

        EntityList<GroupByEntity> nonExistingGroup = multimap.getItems().get("DOES_NOT_EXIST");
        assertNotNull(nonExistingGroup);
        assertEquals(Long.valueOf(0L), nonExistingGroup.getTotalItems());
        assertEquals(List.of(), getDates(nonExistingGroup));

        assertEquals(Long.valueOf(9L), multimap.getTotalItems());
    }

    private static Date utcDate(int year, int month, int day) {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private static List<Date> getDates(EntityList<GroupByEntity> list) {
        return list.getItems()
                .stream()
                .map(GroupByEntity::getDate)
                .collect(Collectors.toList());
    }

    @Test
    public void testAllowGroupBy_explicitlyDisallowGroupBy_doNotAllowQueryWithGroupBy() {
        final TestServiceDao dao = new TestServiceDao(fakedDatastore, TestEntity.class, NoOpMetricSink.INSTANCE, false);
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
            assertEquals(QUERY_ERROR, e.getType());
        }

        // get without groupBy -> successful
        dao.get(ServiceQuery.byId(1L));

        // explicitly allow group by -> successful
        dao.setAllowGroupBy();
        Assert.assertTrue(dao.allowGroupBy);
        dao.get(q);
    }

    @Test
    public void testDuplicateKey_throws() {
        fakedDatastore.getCollection(TestEntity.class)
                .createIndex(new Document("value", 1), new IndexOptions().unique(true));
        final TestServiceDao dao = new TestServiceDao(fakedDatastore, TestEntity.class);

        dao.save(new TestEntity(2L, "nonUniqueValue"));
        assertThrows(RestDslException.class, () -> dao.save(new TestEntity(3L, "nonUniqueValue")));
    }

    @Test
    public void testMultiSortOrder() {
        final TestWithDateDao dao = new TestWithDateDao(fakedDatastore);

        final ServiceQuery<Long> q = ServiceQuery.<Long>builder()
                .order("group,-date")
                .limit(6)
                .build();
        EntityResult<GroupByEntity> result = dao.get(q);
        List<GroupByEntity> items = result.asList();

        assertEquals(6, items.size());

        assertEquals("test01", items.get(0).getGroup());
        assertEquals(utcDate(2025, 1, 1), items.get(0).getDate());

        assertEquals("test02", items.get(1).getGroup());
        assertEquals(utcDate(2025, 1, 2), items.get(1).getDate());

        assertEquals("test02", items.get(2).getGroup());
        assertEquals(utcDate(2025, 1, 1), items.get(2).getDate());

        assertEquals("test03", items.get(3).getGroup());
        assertEquals(utcDate(2025, 1, 3), items.get(3).getDate());

        assertEquals("test03", items.get(4).getGroup());
        assertEquals(utcDate(2025, 1, 2), items.get(4).getDate());

        assertEquals("test03", items.get(5).getGroup());
        assertEquals(utcDate(2025, 1, 1), items.get(5).getDate());
    }

    static class TestServiceDao extends MongoServiceDao<TestEntity, Long> {
        public TestServiceDao(Datastore datastore, Class<TestEntity> entityClazz) {
            super(datastore, entityClazz);
        }

        public TestServiceDao(Datastore datastore, Class<TestEntity> entityClazz, MetricSink metricSink, boolean allowGroupBy) {
            super(datastore, entityClazz, metricSink, allowGroupBy);
        }
    }

    static class TestWithDateDao extends MongoServiceDao<GroupByEntity, Long> {
        public TestWithDateDao(Datastore datastore) {
            super(datastore, GroupByEntity.class);
        }
    }
}
