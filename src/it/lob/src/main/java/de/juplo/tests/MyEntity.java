package de.juplo.tests;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;


/**
 * Taken from https://hibernate.atlassian.net/browse/HHH-9615
 * @author Kai Moritz 
 */
@Entity
public class MyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Convert(converter = MyConverter.class)
    @Lob
    private String status;

}
