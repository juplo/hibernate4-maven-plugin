package main;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import ignored.IgnoredEntity;

@Entity
public class MainEntity
{
  @Id
  @GeneratedValue
  long id;

  @Transient
  IgnoredEntity ignoredEntity;
}
