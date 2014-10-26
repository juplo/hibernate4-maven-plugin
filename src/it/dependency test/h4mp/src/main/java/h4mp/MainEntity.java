package h4mp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

import dependent.DependentEntity;

@Entity
@Audited
public class MainEntity {
	@Id
	@GeneratedValue
	long id;

	@ManyToOne
	DependentEntity dependentEntity;
}
