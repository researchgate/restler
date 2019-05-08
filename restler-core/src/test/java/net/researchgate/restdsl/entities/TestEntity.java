package net.researchgate.restdsl.entities;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import java.util.Date;
import java.util.List;

public class TestEntity {

    @Id
    private Long id;
    private String stringField;
    private Integer integerField;
    private Long longField;
    private List<String> stringList;
    private Date dateField;
    private TestEnum enumField;
    private Boolean booleanField;
    private ObjectId objectIdField;

    public static enum TestEnum {
        enum1,
        enum2,
        enum3
    }

}
