package net.researchgate.restdsl.dao;

import dev.morphia.query.Sort;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class MongoBaseServiceDaoTest {

    @Test
    public void testParseSortString_basic() throws Exception {
        Method parseSortString = MongoBaseServiceDao.class.getDeclaredMethod("parseSortString", String.class);
        parseSortString.setAccessible(true);

        Sort[] sorts = (Sort[]) parseSortString.invoke(null, "field1,-field2, field3 ,-field4");
        assertEquals(4, sorts.length);
        assertSort(sorts[0], "field1", false);
        assertSort(sorts[1], "field2", true);
        assertSort(sorts[2], "field3", false);
        assertSort(sorts[3], "field4", true);
    }

    @Test
    public void testParseSortString_spacesAndEmptyTokensAndNull() throws Exception {
        Method parseSortString = MongoBaseServiceDao.class.getDeclaredMethod("parseSortString", String.class);
        parseSortString.setAccessible(true);

        // leading/trailing spaces
        Sort[] sorts = (Sort[]) parseSortString.invoke(null, " field1 , -field2 ");
        assertEquals(2, sorts.length);
        assertSort(sorts[0], "field1", false);
        assertSort(sorts[1], "field2", true);

        // single token
        sorts = (Sort[]) parseSortString.invoke(null, "-field1");
        assertEquals(1, sorts.length);
        assertSort(sorts[0], "field1", true);

        // empty and whitespace-only -> empty
        sorts = (Sort[]) parseSortString.invoke(null, "");
        assertEquals(0, sorts.length);
        sorts = (Sort[]) parseSortString.invoke(null, "  ");
        assertEquals(0, sorts.length);

        // empty token between commas is skipped
        sorts = (Sort[]) parseSortString.invoke(null, "field1,,field2");
        assertEquals(2, sorts.length);
        assertSort(sorts[0], "field1", false);
        assertSort(sorts[1], "field2", false);
    }

    // Helper that uses reflection to assert that a Sort object targets the
    // expected field and has the specified direction
    // @throws Exception if the Sort class structure is not as expected or if the
    // assertion fails
    private static void assertSort(Sort sort, String expectedField, boolean expectedDesc) throws Exception {
        Class<?> cls = sort.getClass();

        // Morphia 2.x Sort class has private fields "field" and "order" (int: 1 for
        // asc, -1 for desc)
        Field fieldField = cls.getDeclaredField("field");
        fieldField.setAccessible(true);

        Field orderField = cls.getDeclaredField("order");
        orderField.setAccessible(true);

        // Extract actual values
        String actualField = (String) fieldField.get(sort);
        Number orderValue = (Number) orderField.get(sort);
        boolean actualDesc = orderValue.intValue() < 0; // -1 signifies descending

        // Assert that the actual field and direction match the expected values
        if (!expectedField.equals(actualField) || expectedDesc != actualDesc) {
            throw new AssertionError("Sort mismatch. "
                    + "expected field=" + expectedField + " desc=" + expectedDesc
                    + " got field=" + actualField + " desc=" + actualDesc
                    + " rawOrder=" + orderValue);
        }
    }
}
