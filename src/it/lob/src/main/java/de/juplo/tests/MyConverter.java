package de.juplo.tests;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;


/**
 * Taken from https://hibernate.atlassian.net/browse/HHH-9615
 * @author Kai Moritz 
 */
@Converter
public class MyConverter implements AttributeConverter<String, Integer>
{

  @Override
  public Integer convertToDatabaseColumn(String attribute)
  {
    return attribute.length();
  }

  @Override
  public String convertToEntityAttribute(Integer dbData)
  {
    return "";
  }
}
