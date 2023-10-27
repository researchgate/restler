package net.researchgate.restdsl;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity(value = "entity", useDiscriminator = false)
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