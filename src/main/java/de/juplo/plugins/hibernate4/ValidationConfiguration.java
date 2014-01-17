package de.juplo.plugins.hibernate4;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Properties;
import org.hibernate.cfg.Configuration;
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
 */
public class ValidationConfiguration extends Configuration
{
  Class dialectClass;

  public ValidationConfiguration(String dialectClass)
      throws
        ClassNotFoundException
  {
    this.dialectClass = Class.forName(dialectClass);
  }

  @Override
  protected void secondPassCompile() throws MappingException
  {
    super.secondPassCompile();

    try
    {
      /** Thank you, hibernate folks, for making this useful class package private ... */
      Method applyDDL =
          Class
            .forName("org.hibernate.cfg.beanvalidation.TypeSafeActivator")
            .getMethod(
                "applyRelationalConstraints",
                Collection.class,
                Properties.class,
                Dialect.class
                );
      applyDDL.setAccessible(true);
      applyDDL.invoke(
          null,
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
