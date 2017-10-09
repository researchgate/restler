package net.researchgate.restdsl.resources;

import org.glassfish.jersey.uri.PathPattern;
import org.junit.Test;

import java.util.regex.MatchResult;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestBaseServiceResource {

    private PathPattern pattern = new PathPattern(BaseServiceResource.PATH_SEGMENT_PATTERN);

    private void matchSegment(String path, String expectedMatch) {
        MatchResult matchResult = pattern.match(path);
        assertNotNull(matchResult);
        assertEquals(matchResult.group(1), expectedMatch);
    }

    private void noMatchSegment(String path) {
        assertNull(pattern.match(path));
    }

    @Test
    public void testSegmentPathPattern() {
        matchSegment("/", "");
        matchSegment("/segment", "segment");
        matchSegment("/segment/", "segment");

        matchSegment("/segment?arg=val;arg2=val2", "segment?arg=val;arg2=val2");
        matchSegment("/-;a=b;c=d?limit=20", "-;a=b;c=d?limit=20");

        matchSegment("/a/b", "a");
        matchSegment("/a/b/", "a");
        matchSegment("/a/b/?a=v", "a");

        noMatchSegment("");
        noMatchSegment("segment/");
        noMatchSegment("?a=v");
    }
}
