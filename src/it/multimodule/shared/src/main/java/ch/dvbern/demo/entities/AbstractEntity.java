package ch.dvbern.demo.entities;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.Hibernate;

import ch.dvbern.demo.util.Constants;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressWarnings("ClassReferencesSubclass")
@MappedSuperclass
public abstract class AbstractEntity implements Serializable {

	private static final long serialVersionUID = -979317154050183445L;

	@Id
	@Column(unique = true, nullable = false, updatable = false, length = Constants.UUID_LENGTH)
	@Size(min = Constants.UUID_LENGTH, max = Constants.UUID_LENGTH)
	private String id;

	@Version
	@NotNull
	private long version;

	@Size(max = Constants.UUID_LENGTH)
	@Column(nullable = false, length = Constants.UUID_LENGTH)
	private String userErstellt;

	@Size(max = Constants.UUID_LENGTH)
	@Column(nullable = false, length = Constants.UUID_LENGTH)
	private String userMutiert;

	protected AbstractEntity() {
		//da wir teilweise schon eine id brauchen bevor die Entities gespeichert werden initialisieren wir die uuid hier
		id = UUID.randomUUID().toString();
	}

	public String getId() {
		return id;
	}

	public void setId(@Nullable String id) {
		this.id = id;
	}

	// Nullable, da erst im PrePersist gesetzt
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	@Nullable // Nullable, da erst im PrePersist gesetzt
	public String getUserErstellt() {
		return userErstellt;
	}

	public void setUserErstellt(@Nonnull String userErstellt) {
		this.userErstellt = userErstellt;
	}

	@Nullable // Nullable, da erst im PrePersist gesetzt
	public String getUserMutiert() {
		return userMutiert;
	}

	public void setUserMutiert(@Nonnull String userMutiert) {
		this.userMutiert = userMutiert;
	}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	@SuppressFBWarnings(value = "BC_EQUALS_METHOD_SHOULD_WORK_FOR_ALL_OBJECTS", justification = "Es wird Hibernate.getClass genutzt um von Proxies (LazyInit) die konkrete Klasse zu erhalten")
	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
			return false;
		}

		AbstractEntity that = (AbstractEntity) o;

		Objects.requireNonNull(getId());
		Objects.requireNonNull(that.getId());

		return getId().equals(that.getId());
	}

	public int hashCode() {
		return getId() != null ? getId().hashCode() : 0;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("id", getId())
			.toString();
	}
}
