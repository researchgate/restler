package net.researchgate.restler.service.util;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.researchgate.restdsl.types.TypeConverter;
import net.researchgate.restdsl.types.TypeInfoUtil;
import net.researchgate.restler.serde.Rfc3339DateDeserializer;
import net.researchgate.restler.serde.Rfc3339DateSerializer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;
import java.util.Date;

public class TypeInfoUtilTest {
    @BeforeClass
    public static void setUp() {
        TypeInfoUtil.addConverter(new TypeConverter<Date>() {
            @Override
            public Date deserialize(String val) {
                try {
                    return new Rfc3339DateDeserializer().parseRFC3339Date(val);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
            @Override
            public Class<Date> getType() {
                return Date.class;
            }
        });

    }

    private static class TestClassWithSetterAnnotation {

        private Date dateField;

        @JsonSerialize(using = Rfc3339DateSerializer.class, include = JsonSerialize.Inclusion.NON_NULL)
        public Date getDateField() {
            return dateField;
        }

        @JsonDeserialize(using = Rfc3339DateDeserializer.class)
        public void setDateField(Date dateField) {
            this.dateField = dateField;
        }
    }

    private static class TestClassWithDeclarationAnnotation {

        @JsonDeserialize(using = Rfc3339DateDeserializer.class)
        private Date dateField;

        @JsonSerialize(using = Rfc3339DateSerializer.class, include = JsonSerialize.Inclusion.NON_NULL)
        public Date getDateField() {
            return dateField;
        }

        public void setDateField(Date dateField) {
            this.dateField = dateField;
        }
    }

    @Test
    public void testDateDeserializationOnSetter() throws Exception {
        TypeInfoUtil.getValue("2015-03-26T09:08:28.639+0100", "dateField", Date.class, TestClassWithSetterAnnotation.class);
    }

    @Test
    public void testDateDeserializationOnDeclaration() throws Exception {
        TypeInfoUtil.getValue("2015-03-26T09:08:28.639+0100", "dateField", Date.class, TestClassWithDeclarationAnnotation.class);
    }
}
