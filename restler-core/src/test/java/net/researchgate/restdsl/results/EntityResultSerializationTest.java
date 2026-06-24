package net.researchgate.restdsl.results;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Round-trip (serialize + deserialize) tests for all entity container classes,
 * covering the @JsonInclude(NON_NULL) migration from @JsonSerialize(include=NON_NULL).
 *
 * <p>Parameterized over three entity types: String, Integer, and a custom POJO.
 */
@RunWith(Parameterized.class)
public class EntityResultSerializationTest {

    // ---- Simple POJO used as one of the parameterized entity types ----
    public static class SampleEntity {
        public String name;
        public Integer value;

        public SampleEntity() {}

        public SampleEntity(String name, Integer value) {
            this.name = name;
            this.value = value;
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            // label,         singleItem,                itemJsonFragment,  itemValueVerifier
            { "String",
                "hello",
                "\"hello\"",
                (Consumer<Object>) item -> assertEquals("hello", item)
            },
            { "Integer",
                42,
                "42",
                (Consumer<Object>) item -> assertEquals(42, item)
            },
            { "SampleEntity",
                new SampleEntity("x", 7),
                "\"name\"",
                // Jackson deserializes unknown POJOs as LinkedHashMap when T=Object
                (Consumer<Object>) item -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) item;
                    assertEquals("x", m.get("name"));
                    assertEquals(7, m.get("value"));
                }
            },
        });
    }

    private final Object singleItem;
    private final String itemJsonFragment;
    private final Consumer<Object> itemValueVerifier;
    private final ObjectMapper mapper = new ObjectMapper();

    public EntityResultSerializationTest(String label, Object singleItem, String itemJsonFragment,
                                         Consumer<Object> itemValueVerifier) {
        this.singleItem = singleItem;
        this.itemJsonFragment = itemJsonFragment;
        this.itemValueVerifier = itemValueVerifier;
    }

    // ---- Helpers ----

    @SuppressWarnings("unchecked")
    private <T> EntityList<T> singletonList() {
        return new EntityList<>(Collections.singletonList((T) singleItem), 1L);
    }

    @SuppressWarnings("unchecked")
    private <T> EntityMap<T> singletonMap(String key) {
        Map<Object, T> m = new LinkedHashMap<>();
        m.put(key, (T) singleItem);
        return new EntityMap<>(m, 1L);
    }

    @SuppressWarnings("unchecked")
    private <T> EntityMultimap<T> singletonMultimap(String groupKey) {
        Map<Object, EntityList<T>> m = new LinkedHashMap<>();
        m.put(groupKey, new EntityList<>(Collections.singletonList((T) singleItem), 1L));
        return new EntityMultimap<>(m, 1L);
    }

    // ---- EntityResult backed by list: null-omission + round-trip ----

    @Test
    public void entityResult_backedByList_roundTrip() throws Exception {
        EntityList<Object> list = new EntityList<>(Arrays.asList(singleItem, singleItem), 2L);
        EntityResult<Object> original = new EntityResult<>(list);

        String json = mapper.writeValueAsString(original);
        JsonNode node = mapper.readTree(json);

        // null-omission
        assertTrue("list must be present",           node.has("list"));
        assertFalse("null map must be omitted",      node.has("map"));
        assertFalse("null multimap must be omitted", node.has("multimap"));
        assertTrue("item value must appear",         json.contains(itemJsonFragment));

        // deserialization
        EntityResult<Object> deserialized = mapper.readValue(json, new TypeReference<>(){});
        assertEquals(2L, (long) deserialized.getTotalItems());
        assertEquals(2, deserialized.getList().getItems().size());
        deserialized.getList().getItems().forEach(itemValueVerifier);
        assertNull(deserialized.getMap());
        assertNull(deserialized.getMultimap());
    }

    // ---- EntityResult backed by map: null-omission + round-trip ----

    @Test
    public void entityResult_backedByMap_roundTrip() throws Exception {
        EntityResult<Object> original = new EntityResult<>(singletonMap("k1"));

        String json = mapper.writeValueAsString(original);
        JsonNode node = mapper.readTree(json);

        // null-omission
        assertTrue("map must be present",            node.has("map"));
        assertFalse("null list must be omitted",     node.has("list"));
        assertFalse("null multimap must be omitted", node.has("multimap"));
        assertTrue("item value must appear",         json.contains(itemJsonFragment));

        // deserialization
        EntityResult<Object> deserialized = mapper.readValue(json, new TypeReference<>(){});
        assertEquals(1L, (long) deserialized.getTotalItems());
        assertNotNull(deserialized.getMap());
        deserialized.getMap().getItems().values().forEach(itemValueVerifier);
        assertNull(deserialized.getList());
        assertNull(deserialized.getMultimap());
    }

    // ---- EntityResult backed by multimap: null-omission + round-trip ----

    @Test
    public void entityResult_backedByMultimap_roundTrip() throws Exception {
        EntityResult<Object> original = new EntityResult<>(singletonMultimap("g1"));

        String json = mapper.writeValueAsString(original);
        JsonNode node = mapper.readTree(json);

        // null-omission
        assertTrue("multimap must be present",    node.has("multimap"));
        assertFalse("null list must be omitted",  node.has("list"));
        assertFalse("null map must be omitted",   node.has("map"));
        assertTrue("item value must appear",      json.contains(itemJsonFragment));

        // deserialization
        EntityResult<Object> deserialized = mapper.readValue(json, new TypeReference<>(){});
        assertEquals(1L, (long) deserialized.getTotalItems());
        assertNotNull(deserialized.getMultimap());
        for (EntityList<Object> innerList : deserialized.getMultimap().getItems().values()) {
            for (Object item : innerList.getItems()) {
                itemValueVerifier.accept(item);
            }
        }
        assertNull(deserialized.getList());
        assertNull(deserialized.getMap());
    }

    // ---- EntityList: round-trip ----

    @Test
    public void entityList_roundTrip() throws Exception {
        EntityList<Object> original = singletonList();

        String json = mapper.writeValueAsString(original);
        assertTrue("items must be present",      json.contains("\"items\""));
        assertTrue("totalItems must be present", json.contains("\"totalItems\""));
        assertTrue("item value must appear",     json.contains(itemJsonFragment));

        EntityList<Object> deserialized = mapper.readValue(json, new TypeReference<>(){});
        assertEquals(1, deserialized.getItems().size());
        assertEquals(1L, (long) deserialized.getTotalItems());
        itemValueVerifier.accept(deserialized.getItems().get(0));
    }

    // ---- EntityMap: round-trip ----

    @Test
    public void entityMap_roundTrip() throws Exception {
        EntityMap<Object> original = singletonMap("mapKey");

        String json = mapper.writeValueAsString(original);
        assertTrue("items must be present",      json.contains("\"items\""));
        assertTrue("totalItems must be present", json.contains("\"totalItems\""));
        assertTrue("key must appear",            json.contains("\"mapKey\""));
        assertTrue("item value must appear",     json.contains(itemJsonFragment));

        EntityMap<Object> deserialized = mapper.readValue(json, new TypeReference<>(){});
        assertEquals(1, deserialized.getItems().size());
        assertEquals(1L, (long) deserialized.getTotalItems());
        deserialized.getItems().values().forEach(itemValueVerifier);
    }

    // ---- EntityMultimap: round-trip ----

    @Test
    public void entityMultimap_roundTrip() throws Exception {
        EntityMultimap<Object> original = singletonMultimap("groupKey");

        String json = mapper.writeValueAsString(original);
        assertTrue("items must be present",      json.contains("\"items\""));
        assertTrue("totalItems must be present", json.contains("\"totalItems\""));
        assertTrue("group key must appear",      json.contains("\"groupKey\""));
        assertTrue("item value must appear",     json.contains(itemJsonFragment));

        EntityMultimap<Object> deserialized = mapper.readValue(json, new TypeReference<>(){});
        assertEquals(1, deserialized.getItems().size());
        assertEquals(1L, (long) deserialized.getTotalItems());
        for (EntityList<Object> innerList : deserialized.getItems().values()) {
            for (Object item : innerList.getItems()) {
                itemValueVerifier.accept(item);
            }
        }
    }
}
