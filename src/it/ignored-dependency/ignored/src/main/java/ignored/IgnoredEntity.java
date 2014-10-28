package ignored;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class IgnoredEntity
{
  @Id
  @GeneratedValue
  Long id;

  String name;
}
