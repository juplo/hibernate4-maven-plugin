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

  private Class<Dialect> dialectClass;

  public ValidationConfiguration(final String dialectClass)
  {
    try {
        this.dialectClass = (Class<Dialect>) Class.forName(dialectClass);
    } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
    }
  }

  @Override
  protected void secondPassCompile() throws MappingException
  {
    super.secondPassCompile();

    try
    {
      TypeSafeActivatorAccessor.applyRelationalConstraints(
          Validation.buildDefaultValidatorFactory(),
          classes.values(),
          getProperties(),
          dialectClass.newInstance()
          );
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }
}
