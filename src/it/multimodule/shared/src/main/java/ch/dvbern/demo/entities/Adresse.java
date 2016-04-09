package ch.dvbern.demo.entities;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import ch.dvbern.demo.util.Constants;

/**
 * Entitaet zum Speichern von Adressen in der Datenbank.
 */
@Entity
public class Adresse extends AbstractEntity {

	private static final long serialVersionUID = -7687645920281069260L;

	@Size(max = Constants.DB_DEFAULT_MAX_LENGTH)
	@Nonnull
	@NotNull
	@Column(nullable = false, length = Constants.DB_DEFAULT_MAX_LENGTH)
	private String strasse = "";

	@Size(max = Constants.DB_DEFAULT_SHORT_LENGTH)
	@Nullable
	@Column(nullable = true, length = Constants.DB_DEFAULT_SHORT_LENGTH)
	private String hausnummer = null;

	@Size(max = Constants.DB_DEFAULT_MAX_LENGTH)
	@Nullable
	@Column(nullable = true, length = Constants.DB_DEFAULT_MAX_LENGTH)
	private String zusatzzeile = null;

	@Size(max = Constants.DB_DEFAULT_SHORT_LENGTH)
	@Nonnull
	@NotNull
	@Column(nullable = false, length = Constants.DB_DEFAULT_SHORT_LENGTH)
	private String plz = "";

	@Size(max = Constants.DB_DEFAULT_MAX_LENGTH)
	@Nonnull
	@NotNull
	@Column(nullable = false, length = Constants.DB_DEFAULT_MAX_LENGTH)
	private String ort = "";

	@ManyToMany(mappedBy = "adressen")
	private List<Person> personen = new ArrayList<>();

	@Nonnull
	public String getStrasse() {
		return strasse;
	}

	public void setStrasse(@Nonnull final String strasse) {
		this.strasse = strasse;
	}

	@Nullable
	public String getHausnummer() {
		return hausnummer;
	}

	public void setHausnummer(@Nullable final String hausnummer) {
		this.hausnummer = hausnummer;
	}

	@Nullable
	public String getZusatzzeile() {
		return zusatzzeile;
	}

	public void setZusatzzeile(@Nullable final String zusatzzeile) {
		this.zusatzzeile = zusatzzeile;
	}

	@Nonnull
	public String getPlz() {
		return plz;
	}

	public void setPlz(@Nonnull final String plz) {
		this.plz = plz;
	}

	@Nonnull
	public String getOrt() {
		return ort;
	}

	public void setOrt(@Nonnull final String ort) {
		this.ort = ort;
	}

	public List<Person> getPersonen() {
		return personen;
	}

	public void setPersonen(final List<Person> personen) {
		this.personen = personen;
	}
}
