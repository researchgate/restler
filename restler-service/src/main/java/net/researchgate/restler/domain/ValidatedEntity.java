package net.researchgate.restler.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import net.researchgate.restler.serde.ObjectIdDeserializer;
import net.researchgate.restler.serde.ObjectIdSerializer;
import org.bson.types.ObjectId;

import java.util.Objects;

/**
 * Entity to test validations
 */
@Entity("validatedEntities")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidatedEntity {
    @Id
    @JsonSerialize(using = ObjectIdSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId id;
    private String notNullField;
    private Integer positiveField;

    public ValidatedEntity() {
    }

    public ValidatedEntity(ObjectId id, String notNullField, Integer positiveField) {
        this.id = id;
        this.notNullField = notNullField;
        this.positiveField = positiveField;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getNotNullField() {
        return notNullField;
    }

    public void setNotNullField(String notNullField) {
        this.notNullField = notNullField;
    }

    public Integer getPositiveField() {
        return positiveField;
    }

    public void setPositiveField(Integer positiveField) {
        this.positiveField = positiveField;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof ValidatedEntity that)) return false;

        return Objects.equals(id, that.id) && Objects.equals(notNullField, that.notNullField) && Objects.equals(positiveField, that.positiveField);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(notNullField);
        result = 31 * result + Objects.hashCode(positiveField);
        return result;
    }

    @Override
    public String toString() {
        return "ValidatedEntity{" +
                "id=" + id +
                ", notNullField='" + notNullField + '\'' +
                ", positiveField=" + positiveField +
                '}';
    }
}
