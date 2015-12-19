package b;

import java.util.Set;
import javax.persistence.Table;


@Table(name = "WRONG_NAME")
public class Customer extends Person
{
  public enum CustomerLevel { BRONZE, SILVER, GOLD };
  private Set<Sale> purchases;
  private String email;
  private CustomerLevel level = CustomerLevel.BRONZE;
}
