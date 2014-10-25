package dependent;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

@Entity
@Audited
public class DependentEntity {
	@Id
	@GeneratedValue
	long id;

	String name;
}
