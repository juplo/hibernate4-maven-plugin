package b;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;
import java.util.UUID;


public class Sale
{
  private String id;
  private BigDecimal amount;
  private Date dateTime;
  private Set<Clerk> salesClerks;
  private Customer customer;

  public Sale()
  {
    this(UUID.randomUUID().toString());
  }

  public Sale(String id)
  {
    this.id = id;
  }
}