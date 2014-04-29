package de.juplo.plugins.hibernate4.tests;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.Index;

@Entity
@Table(name = "test_simple")
@org.hibernate.annotations.Table(
        appliesTo="test_simple",
        indexes = {
            @Index(name="idx_test_simple_tuple", columnNames={"sources", "uuid"} ),
        }
)
public class SimplestMavenHib4Test {

    private String sources;

    @Lob
    private String content;

    @Id
    @Column (length=36)
    private String uuid;

    @Column(name = "externalid", length=148)
    private String externalXyzId;
}
