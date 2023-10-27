package net.researchgate.restdsl.util;

import net.researchgate.restdsl.entities.Account;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.types.TypeInfoUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Testing conversion methods
 */
public class TypeInfoUtilTest {

    @Test
    public void testSimple() {
        assertEquals(123L, (Object) TypeInfoUtil.getValue("123", Long.class));

        try {
            TypeInfoUtil.getValue("123X", Long.class);
            fail();
        } catch (RestDslException e) {
            assertEquals(RestDslException.Type.PARAMS_ERROR, e.getType());
        }

    }
}
