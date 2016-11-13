package io.blep;

import javax.persistence.*;

/**
 * @author blep
 */
@Entity
public class MyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Convert(converter = MyConverter.class)
    @Lob
    private String status;

    @Converter
    public static class MyConverter implements AttributeConverter<String, Integer> {

        @Override
        public Integer convertToDatabaseColumn(String attribute) {
            return attribute.length();
        }

        @Override
        public String convertToEntityAttribute(Integer dbData) {
            return "";
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
