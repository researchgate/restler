package net.researchgate.restdsl;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

import java.util.Date;

@Entity(value = "groupByEntities", useDiscriminator = false)
public class GroupByEntity {
    @Id
    ObjectId id;
    String group;
    Date date;

    public GroupByEntity() {
    }

    public GroupByEntity(ObjectId id, String group, Date date) {
        this.id = id;
        this.group = group;
        this.date = date;
    }

    public ObjectId getId() {
        return id;
    }

    public String getGroup() {
        return group;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "GroupByEntity{" +
                "id=" + id +
                ", group='" + group + '\'' +
                ", date=" + date +
                '}';
    }
}