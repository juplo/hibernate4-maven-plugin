package h4mp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

@Entity
@Audited
public class MainEntity {
	@Id
	@GeneratedValue
	long id;

	String str;
}
