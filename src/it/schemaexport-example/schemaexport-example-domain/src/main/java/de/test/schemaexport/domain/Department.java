package de.test.schemaexport.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.Type;

/**
 * Abteilungsklasse (Generator-Beispielcode).
 * 
 * copyright
 *
 */
@Entity
@Table(name = "ABTEILUNG")
public class Department {

	@Id
	@Column(name = "OID")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long oid;
	
	@Column(name = "name", nullable = false)
	private String name;

	@Type(type = "genderType")
	private Gender gender;

	public long getOid() {
		return oid;
	}

	public void setOid(long oid) {
		this.oid = oid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	
}
