package ch.dvbern.demo.entities;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static ch.dvbern.demo.util.Constants.DB_DEFAULT_MAX_LENGTH;

/**
 * Entity fuer personendaten
 */
@Entity
@Table(uniqueConstraints = {
	@UniqueConstraint(name = "person_uc1", columnNames = "vorname"),
	@UniqueConstraint(name = "person_uc2", columnNames = "userErstellt"),
	@UniqueConstraint(name = "person_uc3", columnNames = { "userErstellt", "vorname" }) // FIXME funktioniert nicht
})
public class Person extends AbstractEntity {

	private static final long serialVersionUID = -9032257320578372570L;

	@Nonnull
	@Size(min = 1, max = DB_DEFAULT_MAX_LENGTH)
	@Column(nullable = false)
	@NotNull
	private String vorname = "";

	@Nonnull
	@Size(min = 1, max = DB_DEFAULT_MAX_LENGTH)
	@NotNull
	@Column(nullable = false)
	private String nachname = "";

	@Nonnull
	@ManyToMany
	@JoinTable(
		name = "personen_adressen",
		joinColumns = @JoinColumn(name = "person_id", foreignKey = @ForeignKey(name = "person_fk1")),
		inverseJoinColumns = @JoinColumn(name = "adresse_id", foreignKey = @ForeignKey(name = "adresse_fk1")),
		indexes = {
			@Index(name = "personen_adressen_ix1", columnList = "person_id"),
			@Index(name = "personen_adressen_ix2", columnList = "adresse_id")
		}
	)
//	@org.hibernate.annotations.ForeignKey(name = "person_fk1", inverseName = "adresse_fk1") // TODO ohne die Hibernate spez. Annotation funktioniert das Naming der Foreign Keys nicht
	private List<Adresse> adressen = new ArrayList<>();


	@Nonnull
	public String getVorname() {
		return vorname;
	}

	public void setVorname(@Nonnull final String vorname) {
		this.vorname = vorname;
	}

	@Nonnull
	public String getNachname() {
		return nachname;
	}

	public void setNachname(@Nonnull final String nachname) {
		this.nachname = nachname;
	}

	@Nonnull
	public List<Adresse> getAdressen() {
		return adressen;
	}

	public void setAdressen(@Nonnull final List<Adresse> adressen) {
		this.adressen = adressen;
	}
}
