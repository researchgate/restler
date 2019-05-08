package net.researchgate.restdsl.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.researchgate.restdsl.entities.TestEntity;
import net.researchgate.restdsl.exceptions.RestDslException;
import net.researchgate.restdsl.queries.ServiceQuery;
import net.researchgate.restdsl.queries.ServiceQueryParams;
import org.glassfish.jersey.internal.util.collection.ImmutableMultivaluedMap;
import org.glassfish.jersey.uri.UriComponent;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

public class RequestUtilTest {


    @Test
    public void testParseRequest_basic() {
        ServiceQuery<Long> result = parseSimplePathSegment("-");

        Assert.assertEquals(ServiceQueryParams.DEFAULT_QUERY_PARAMS.getDefaultLimit(), result.getLimit());
        Assert.assertEquals(ServiceQueryParams.DEFAULT_QUERY_PARAMS.getDefaultFields(), result.getFields());
        Assert.assertEquals(0, result.getOffset());
        Assert.assertNull(result.getIdList());

    }

    //TODO: lets dedup here
    @Test
    public void testParseRequest_duplicateIdsProvided_doNotDeduplicate() {
        ServiceQuery<Long> result = parseSimplePathSegment("1,2,3,3,3");
        Assert.assertEquals(Lists.newArrayList(1L, 2L, 3L, 3L, 3L), result.getIdList());
    }

    // TODO trim().ignoreEmpty and adapt test !
    @Test(expected = RestDslException.class)
    public void testParseRequest_emptyIdsProvided_failOnEmpty() {
        ServiceQuery<Long> result = parseSimplePathSegment("1,,2, , ,,,");
        Assert.assertEquals(Lists.newArrayList(1L, 2L, 3L), result.getIdList());
    }

    @Test
    public void testParseRequest_doubleKey_matchEither() {
        ServiceQuery<Long> result = parseSimplePathSegment("-;stringField=foo;stringField=bar");
        Assert.assertEquals(Sets.newHashSet("foo", "bar"), result.getCriteria().get("stringField"));
    }

    @Test
    public void testParseRequest_multipleValues_matchEither() {
        ServiceQuery<Long> result = parseSimplePathSegment("-;stringField=foo,bar");
        Assert.assertEquals(Sets.newHashSet("foo", "bar"), result.getCriteria().get("stringField"));
    }

    @Test(expected = RestDslException.class)
    public void testParseRequest_unknownKey_throwException() {
        ServiceQuery<Long> result = parseSimplePathSegment("-;nonExistingKey=foo");
    }


    @Test()
    public void testParseRequest_providedEnum_accept() {
        ServiceQuery<Long> result = parseSimplePathSegment("-;enumField=enum1,enum2");
    }

    @Test()
    public void testParseRequest_providedDate_accept() {
        ServiceQuery<Long> result = parseSimplePathSegment("-;dateField=1254856445");
    }


    @Test()
    public void testParseRequest_providedBoolean_accept() {
        ServiceQuery<Long> result = parseSimplePathSegment("-;booleanField=true,false");
    }

    @Test()
    public void testParseRequest_providedObjectId_accept() {
        ServiceQuery<Long> result = parseSimplePathSegment("-;objectIdField=5b5207c5603c2c1d401cc472");
    }

    @Test()
    public void testParseRequest_providedInteger_accept() {
        ServiceQuery<Long> result = parseSimplePathSegment("-;integerField=-4,0,1024");
    }


    @Test(expected = RestDslException.class)
    public void testParseRequest_ValueTypeDoesNotMatch_throwException() {
        ServiceQuery<Long> result = parseSimplePathSegment("-;longField=notANumber");
        Assert.assertEquals(Sets.newHashSet("foo", "bar"), result.getCriteria().get("stringField"));
    }

    @Test()
    public void testParseRequest_multpleKeyConstraints_combineWithAnd() {
        ServiceQuery<Long> result = parseSimplePathSegment("1,2,3;longField=42;stringField=foo");
        Assert.assertEquals(Sets.newHashSet("foo"), result.getCriteria().get("stringField"));
        Assert.assertEquals(Sets.newHashSet(42L), result.getCriteria().get("longField"));
        Assert.assertEquals(Lists.newArrayList(1L, 2L, 3L), result.getIdList());
    }

    private static ServiceQuery<Long> parseSimplePathSegment(String clientQuery) {
        UriInfo emptyUriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(emptyUriInfo.getQueryParameters()).thenReturn(ImmutableMultivaluedMap.empty());

        PathSegment pathSegment = UriComponent.decodePath(clientQuery, true).get(0);


        ServiceQuery<Long> query = RequestUtil.parseRequest(TestEntity.class, Long.class, pathSegment, emptyUriInfo,
                ServiceQueryParams.DEFAULT_QUERY_PARAMS);
        System.out.println("Created query: " + query);
        return query;
    }

}