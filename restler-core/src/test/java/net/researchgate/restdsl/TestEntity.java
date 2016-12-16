package net.researchgate.restdsl;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity(value = "entity", noClassnameStored = true)
public class TestEntity {
    @Id
    Long id;
    String value;

    public TestEntity() {
    }

    public TestEntity(Long id, String value) {
        this.id = id;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }
}