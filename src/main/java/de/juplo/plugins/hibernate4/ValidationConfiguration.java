package de.juplo.plugins.hibernate4;

import javax.validation.Validation;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.beanvalidation.TypeSafeActivatorAccessor;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.source.MappingException;


/**
 * This integration is usually performed by BeanValidationIntegrator.
 * Unfortunately, that integration will only be activated upon
 * initialization of the ServiceRegistry, which initializes
 * DatasourceConnectionProviderImpl, which looks up the datasource,
 * which requires a JNDI context ...
 * We therefore reimplement the relevant parts of BeanValidatorIntegrator.
 * Since that must occur after secondPassCompile(), which is invoked by
 * Configuration.generateSchemaCreationScript, which is invoked by
 * SchemaExport, some fancy subclassing is needed to invoke the integration
 * at the right time.
 * @author Mark Robinson <mark@mrobinson.ca>
 * @author Frank Schimmel <frank.schimmel@cm4all.com>
 */
public class ValidationConfiguration extends Configuration
{
  private static final long serialVersionUID = 1L;


  @Override
  protected void secondPassCompile() throws MappingException
  {
    super.secondPassCompile();

    try
    {
      Class<?> dialectClass = Class.forName( getProperty(Hbm2DdlMojo.DIALECT), true, Thread.currentThread().getContextClassLoader()); 
      TypeSafeActivatorAccessor.applyRelationalConstraints(
          Validation.buildDefaultValidatorFactory(),
          classes.values(),
          getProperties(),
          (Dialect)dialectClass.newInstance()
          );
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  public String getTypeDefs()
  {
    return typeDefs.entrySet().toString();
  }
}
