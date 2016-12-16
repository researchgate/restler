package net.researchgate.restdsl.model;

import net.researchgate.restdsl.TestEntity;
import net.researchgate.restdsl.dao.PersistentServiceDao;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.results.EntityResult;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ServiceModelTest {


    private TestModel model;
    private PersistentServiceDao<TestEntity, Long> mockedDao;

    @Before
    public void setUp() {
        mockedDao = Mockito.mock(PersistentServiceDao.class);
        model = new TestModel(mockedDao);
    }

    @Test
    public void testNotModified() {
        setUp();
        
        TestEntity dbEntry = new TestEntity(5L, "v");

        // not set
        model.ensureNotModified(TestEntity::getId, dbEntry, new TestEntity(null, "v"));

        // set to same value
        model.ensureNotModified(TestEntity::getId, dbEntry, new TestEntity(5L, "v"));

        // set to different value
        try {
            model.ensureNotModified(TestEntity::getId, dbEntry, new TestEntity(6L, "v"));
            fail();
        } catch (RestDslException e) {
            System.out.println(e);
            assertEquals(RestDslException.Type.ENTITY_ERROR, e.getType());
        }
    }

    @Test
    public void testCheckNotSet() {
        setUp();
        // not set
        model.ensureNotSet(new TestEntity().getId(), "entityId");

        // set
        TestEntity patch = new TestEntity(1L, "val");
        try {
            model.ensureNotSet(patch.getId(), "entityId");
            fail();
        } catch (RestDslException e) {
            System.out.println(e);
            assertEquals(RestDslException.Type.ENTITY_ERROR, e.getType());
        }
    }

    @Test
    public void testCheckNotNull() throws Exception {
        setUp();
        // set
        model.ensureNotNull(new TestEntity(1L, "val").getId(), "entityId");

        // not set
        try {
            model.ensureNotNull(new TestEntity().getId(), "entityId");
            fail();
        } catch (RestDslException e) {
            System.out.println(e);
            assertEquals(RestDslException.Type.ENTITY_ERROR, e.getType());
        }
    }

    @Test
    @Ignore // to catch null id arguments instead of
    public void testGet() throws Exception {
        Mockito.when(mockedDao.get(Mockito.any()))
                .thenReturn(new EntityResult<>(Collections.emptyList(), 0L));

        model.get((Long) null);

        Mockito.verify(mockedDao, Mockito.never())
                .get(Mockito.any());
    }

}