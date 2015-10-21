package de.juplo.plugins.hibernate;


import com.pyx4j.log4j.MavenLogAppender;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.spi.PersistenceUnitTransactionType;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataImplementor;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.IMPLICIT_NAMING_STRATEGY;
import static org.hibernate.cfg.AvailableSettings.PASS;
import static org.hibernate.cfg.AvailableSettings.PHYSICAL_NAMING_STRATEGY;
import static org.hibernate.cfg.AvailableSettings.USER;
import static org.hibernate.cfg.AvailableSettings.URL;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.util.config.ConfigurationException;
import static org.hibernate.jpa.AvailableSettings.JDBC_DRIVER;
import static org.hibernate.jpa.AvailableSettings.JDBC_PASSWORD;
import static org.hibernate.jpa.AvailableSettings.JDBC_URL;
import static org.hibernate.jpa.AvailableSettings.JDBC_USER;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.ProviderChecker;
import org.scannotation.AnnotationDB;


/**
 * Baseclass with common attributes and methods.
 *
 * @phase process-classes
 * @threadSafe
 * @requiresDependencyResolution runtime
 */
public abstract class AbstractSchemaMojo extends AbstractMojo
{
  public final static String EXPORT_SKIPPED_PROPERTY = "hibernate.export.skipped";

  private final static Pattern SPLIT = Pattern.compile("[^,\\s]+");


  /**
   * The maven project.
   * <p>
   * Only needed internally.
   *
   * @parameter property="project"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * Build-directory.
   * <p>
   * Only needed internally.
   *
   * @parameter property="project.build.directory"
   * @required
   * @readonly
   */
  String buildDirectory;

  /**
   * Classes-Directory to scan.
   * <p>
   * This parameter defaults to the maven build-output-directory for classes.
   * Additionally, all dependencies are scanned for annotated classes.
   *
   * @parameter property="project.build.outputDirectory"
   * @since 1.0
   */
  private String outputDirectory;

  /**
   * Whether to scan test-classes too, or not.
   * <p>
   * If this parameter is set to <code>true</code> the test-classes of the
   * artifact will be scanned for hibernate-annotated classes additionally.
   *
   * @parameter property="hibernate.export.scan_testclasses" default-value="false"
   * @since 1.0.1
   */
  private boolean scanTestClasses;

  /**
   * Dependency-Scopes, that should be scanned for annotated classes.
   * <p>
   * By default, only dependencies in the scope <code>compile</code> are
   * scanned for annotated classes. Multiple scopes can be seperated by
   * white space or commas.
   * <p>md5s
   * If you do not want any dependencies to be scanned for annotated
   * classes, set this parameter to <code>none</code>.
   * <p>
   * The plugin does not scan for annotated classes in transitive
   * dependencies. If some of your annotated classes are hidden in a
   * transitive dependency, you can simply add that dependency explicitly.
   *
   * @parameter property="hibernate.export.scan_dependencies" default-value="compile"
   * @since 1.0.3
   */
  private String scanDependencies;

  /**
   * Test-Classes-Directory to scan.
   * <p>
   * This parameter defaults to the maven build-output-directory for
   * test-classes.
   * <p>
   * This parameter is only used, when <code>scanTestClasses</code> is set
   * to <code>true</code>!
   *
   * @parameter property="project.build.testOutputDirectory"
   * @since 1.0.2
   */
  private String testOutputDirectory;

  /**
   * Skip execution
   * <p>
   * If set to <code>true</code>, the execution is skipped.
   * <p>
   * A skipped execution is signaled via the maven-property
   * <code>${hibernate.export.skipped}</code>.
   * <p>
   * The execution is skipped automatically, if no modified or newly added
   * annotated classes are found and the dialect was not changed.
   *
   * @parameter property="hibernate.skip" default-value="${maven.test.skip}"
   * @since 1.0
   */
  private boolean skip;

  /**
   * Force execution
   * <p>
   * Force execution, even if no modified or newly added annotated classes
   * where found and the dialect was not changed.
   * <p>
   * <code>skip</code> takes precedence over <code>force</code>.
   *
   * @parameter property="hibernate.export.force" default-value="false"
   * @since 1.0
   */
  private boolean force;

  /**
   * SQL-Driver name.
   *
   * @parameter property="hibernate.connection.driver_class"
   * @since 1.0
   */
  private String driver;

  /**
   * Database URL.
   *
   * @parameter property="hibernate.connection.url"
   * @since 1.0
   */
  private String url;

  /**
   * Database username
   *
   * @parameter property="hibernate.connection.username"
   * @since 1.0
   */
  private String username;

  /**
   * Database password
   *
   * @parameter property="hibernate.connection.password"
   * @since 1.0
   */
  private String password;

  /**
   * Hibernate dialect.
   *
   * @parameter property="hibernate.dialect"
   * @since 1.0
   */
  private String dialect;

  /**
   * Implicit naming strategy
   *
   * @parameter property=IMPLICIT_NAMING_STRATEGY
   * @since 2.0
   */
  private String implicitNamingStrategy;

  /**
   * Physical naming strategy
   *
   * @parameter property=PHYSICAL_NAMING_STRATEGY
   * @since 2.0
   */
  private String physicalNamingStrategy;

  /**
   * Path to a file or name of a ressource with hibernate properties.
   * If this parameter is specified, the plugin will try to load configuration
   * values from a file with the given path or a ressource on the classpath with
   * the given name. If both fails, the execution of the plugin will fail.
   * <p>
   * If this parameter is not set the plugin will load configuration values
   * from a ressource named <code>hibernate.properties</code> on the classpath,
   * if it is present, but will not fail if there is no such ressource.
   * <p>
   * During ressource-lookup, the test-classpath takes precedence.
   *
   * @parameter
   * @since 1.0
   */
  private String hibernateProperties;

  /**
   * Path to Hibernate configuration file (.cfg.xml).
   * If this parameter is specified, the plugin will try to load configuration
   * values from a file with the given path or a ressource on the classpath with
   * the given name. If both fails, the execution of the plugin will fail.
   * <p>
   * If this parameter is not set the plugin will load configuration values
   * from a ressource named <code>hibernate.cfg.xml</code> on the classpath,
   * if it is present, but will not fail if there is no such ressource.
   * <p>
   * During ressource-lookup, the test-classpath takes precedence.
   * <p>
   * Settings in this file will overwrite settings in the properties file.
   *
   * @parameter
   * @since 1.1.0
   */
  private String hibernateConfig;

  /**
   * Name of the persistence-unit.
   * If this parameter is specified, the plugin will try to load configuration
   * values from a persistence-unit with the specified name. If no such
   * persistence-unit can be found, the plugin will throw an exception.
   * <p>
   * If this parameter is not set and there is only one persistence-unit
   * available, that unit will be used automatically. But if this parameter is
   * not set and there are multiple persistence-units available on,
   * the class-path, the execution of the plugin will fail.
   * <p>
   * Settings in this file will overwrite settings in the properties or the
   * configuration file.
   *
   * @parameter
   * @since 1.1.0
   */
  private String persistenceUnit;

  /**
   * List of Hibernate-Mapping-Files (XML).
   * Multiple files can be separated with white-spaces and/or commas.
   *
   * @parameter property="hibernate.mapping"
   * @since 1.0.2
   */
  private String mappings;


  @Override
  public final void execute()
    throws
      MojoFailureException,
      MojoExecutionException
  {
    if (skip)
    {
      getLog().info("Execution of hibernate-maven-plugin was skipped!");
      project.getProperties().setProperty(EXPORT_SKIPPED_PROPERTY, "true");
      return;
    }

    ModificationTracker tracker;
    try
    {
      tracker = new ModificationTracker(buildDirectory, getLog());
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new MojoFailureException("Digest-Algorithm MD5 is missing!", e);
    }

    SimpleConnectionProvider connectionProvider =
        new SimpleConnectionProvider(getLog());

    try
    {
      /** Start extended logging */
      MavenLogAppender.startPluginLog(this);

      /** Load checksums for old mapping and configuration */
      tracker.load();

      /** Create a BootstrapServiceRegistry with special ClassLoader */
      BootstrapServiceRegistry bootstrapServiceRegitry =
          new BootstrapServiceRegistryBuilder()
              .applyClassLoader(createClassLoader())
              .build();
      ClassLoaderService classLoaderService =
          bootstrapServiceRegitry.getService(ClassLoaderService.class);

      Properties properties = new Properties();
      ConfigLoader configLoader = new ConfigLoader(bootstrapServiceRegitry);

      /** Loading and merging configuration */
      properties.putAll(loadProperties(configLoader));
      properties.putAll(loadConfig(configLoader));
      properties.putAll(loadPersistenceUnit(classLoaderService, properties));

      /** Overwriting/Completing configuration */
      configure(properties);

      /** Check configuration for modifications */
      if(tracker.check(properties))
        getLog().debug("Configuration has changed.");
      else
        getLog().debug("Configuration unchanged.");

      /** Configure Hibernate */
      StandardServiceRegistry serviceRegistry =
          new StandardServiceRegistryBuilder(bootstrapServiceRegitry)
              .applySettings(properties)
              .addService(ConnectionProvider.class, connectionProvider)
              .build();

      /** Load Mappings */
      MetadataSources sources = new MetadataSources(serviceRegistry);
      addAnnotatedClasses(sources, classLoaderService, tracker);
      addMappings(sources, tracker);

      /** Skip execution, if mapping and configuration is unchanged */
      if (!tracker.modified())
      {
        getLog().info(
            "Mapping and configuration unchanged."
            );
        if (force)
          getLog().info("Schema generation is forced!");
        else
        {
          getLog().info("Skipping schema generation!");
          project.getProperties().setProperty(EXPORT_SKIPPED_PROPERTY, "true");
          return;
        }
      }


      /** Create a connection, if sufficient configuration infromation is available */
      connectionProvider.open(classLoaderService, properties);

      MetadataBuilder metadataBuilder = sources.getMetadataBuilder();

      StrategySelector strategySelector =
          serviceRegistry.getService(StrategySelector.class);

      if (properties.containsKey(IMPLICIT_NAMING_STRATEGY))
      {
        metadataBuilder.applyImplicitNamingStrategy(
            strategySelector.resolveStrategy(
                ImplicitNamingStrategy.class,
                properties.getProperty(IMPLICIT_NAMING_STRATEGY)
                )
            );
      }

      if (properties.containsKey(PHYSICAL_NAMING_STRATEGY))
      {
        metadataBuilder.applyPhysicalNamingStrategy(
            strategySelector.resolveStrategy(
                PhysicalNamingStrategy.class,
                properties.getProperty(PHYSICAL_NAMING_STRATEGY)
                )
            );
      }

      build((MetadataImplementor)metadataBuilder.build());
    }
    finally
    {
      /** Remember mappings and configuration */
      tracker.save();

      /** Close the connection - if one was opened */
      connectionProvider.close();

      /** Stop Log-Capturing */
      MavenLogAppender.endPluginLog(this);
    }
  }


  abstract void build(MetadataImplementor metadata)
    throws
      MojoFailureException,
      MojoExecutionException;


  private URLClassLoader createClassLoader() throws MojoExecutionException
  {
    try
    {
      getLog().debug("Creating ClassLoader for project-dependencies...");
      List<String> classpathFiles = project.getCompileClasspathElements();
      if (scanTestClasses)
        classpathFiles.addAll(project.getTestClasspathElements());
      List<URL> urls = new LinkedList<URL>();
      File file;
      file = new File(testOutputDirectory);
      if (!file.exists())
      {
        getLog().info("creating test-output-directory: " + testOutputDirectory);
        file.mkdirs();
      }
      urls.add(file.toURI().toURL());
      file = new File(outputDirectory);
      if (!file.exists())
      {
        getLog().info("creating output-directory: " + outputDirectory);
        file.mkdirs();
      }
      urls.add(file.toURI().toURL());
      for (String pathElement : classpathFiles)
      {
        getLog().debug("Dependency: " + pathElement);
        urls.add(new File(pathElement).toURI().toURL());
      }
      return
          new URLClassLoader(
              urls.toArray(new URL[urls.size()]),
              getClass().getClassLoader()
              );
    }
    catch (Exception e)
    {
      getLog().error("Error while creating ClassLoader!", e);
      throw new MojoExecutionException(e.getMessage());
    }
  }

  private Map loadProperties(ConfigLoader configLoader)
      throws
        MojoExecutionException
  {
    /** Try to read configuration from properties-file */
    if (hibernateProperties == null)
    {
      try
      {
        return configLoader.loadProperties("hibernate.properties");
      }
      catch (ConfigurationException e)
      {
        getLog().debug(e.getMessage());
        return Collections.EMPTY_MAP;
      }
    }
    else
    {
      try
      {
        File file = new File(hibernateProperties);
        if (file.exists())
        {
          getLog().info("Reading settings from file " + hibernateProperties + "...");
          return configLoader.loadProperties(file);
        }
        else
          return configLoader.loadProperties(hibernateProperties);
      }
      catch (ConfigurationException e)
      {
        getLog().error("Error while reading properties!", e);
        throw new MojoExecutionException(e.getMessage());
      }
    }
  }

  private Map loadConfig(ConfigLoader configLoader)
      throws MojoExecutionException
  {
    /** Try to read configuration from configuration-file */
    if (hibernateConfig == null)
    {
      try
      {
        return
            configLoader
                .loadConfigXmlResource("hibernate.cfg.xml")
                .getConfigurationValues();
      }
      catch (ConfigurationException e)
      {
        getLog().debug(e.getMessage());
        return Collections.EMPTY_MAP;
      }
    }
    else
    {
      try
      {
        File file = new File(hibernateConfig);
        if (file.exists())
        {
          getLog().info("Reading configuration from file " + hibernateConfig + "...");
          return configLoader.loadConfigXmlFile(file).getConfigurationValues();
        }
        else
          return
              configLoader
                  .loadConfigXmlResource(hibernateConfig)
                  .getConfigurationValues();
      }
      catch (ConfigurationException e)
      {
        getLog().error("Error while reading configuration!", e);
        throw new MojoExecutionException(e.getMessage());
      }
    }
  }

  private void configure(Properties properties)
      throws MojoFailureException
  {
    /** Overwrite values from properties-file or set, if given */

    configure(properties, driver, DRIVER, JDBC_DRIVER);
    configure(properties, url, URL, JDBC_URL);
    configure(properties, username, USER, JDBC_USER);
    configure(properties, password, PASS, JDBC_PASSWORD);
    configure(properties, dialect, DIALECT);
    configure(properties, implicitNamingStrategy, IMPLICIT_NAMING_STRATEGY);
    configure(properties, physicalNamingStrategy, PHYSICAL_NAMING_STRATEGY);

    if (properties.isEmpty())
    {
      getLog().error("No properties set!");
      throw new MojoFailureException("Hibernate configuration is missing!");
    }

    getLog().info("Gathered hibernate-configuration (turn on debugging for details):");
    for (Entry<Object,Object> entry : properties.entrySet())
      getLog().info("  " + entry.getKey() + " = " + entry.getValue());
  }

  private void configure(
      Properties properties,
      String value,
      String key,
      String alternativeKey
      )
  {
    configure(properties, value, key);
    if (properties.containsKey(key) && properties.containsKey(alternativeKey))
    {
      getLog().warn(
          "Ignoring property " + alternativeKey + "=" +
          properties.getProperty(alternativeKey) + " in favour for property " +
          key + "=" + properties.getProperty(key)
          );
      properties.remove(JDBC_DRIVER);
    }
  }

  private void configure(Properties properties, String value, String key)
  {
    if (value != null)
    {
      if (properties.containsKey(key))
        getLog().debug(
            "Overwriting property " + key + "=" + properties.getProperty(key) +
            " with the value " + value
            );
      else
        getLog().debug("Using the value " + value + " for property " + key);
      properties.setProperty(key, value);
    }
  }

  private void addMappings(MetadataSources sources, ModificationTracker tracker)
      throws MojoFailureException
  {
    getLog().debug("Adding explicitly configured mappings...");
    if (mappings != null)
    {
      try
      {
        for (String filename : mappings.split("[\\s,]+"))
        {
          // First try the filename as absolute/relative path
          File file = new File(filename);
          if (!file.exists())
          {
            // If the file was not found, search for it in the resource-directories
            for (Resource resource : project.getResources())
            {
              file = new File(resource.getDirectory() + File.separator + filename);
              if (file.exists())
                break;
            }
          }
          if (file.exists())
          {
            if (file.isDirectory())
              // TODO: add support to read all mappings under a directory
              throw new MojoFailureException(file.getAbsolutePath() + " is a directory");
            if (tracker.check(filename, new FileInputStream(file)))
              getLog().debug("Found new or modified mapping-file: " + filename);
            else
              getLog().debug("mapping-file unchanged: " + filename);

            sources.addFile(file);
          }
          else
            throw new MojoFailureException("File " + filename + " could not be found in any of the configured resource-directories!");
        }
      }
      catch (IOException e)
      {
        throw new MojoFailureException("Cannot calculate MD5 sums!", e);
      }
    }
  }

  private void addAnnotatedClasses(
      MetadataSources sources,
      ClassLoaderService classLoaderService,
      ModificationTracker tracker
      )
      throws
        MojoFailureException,
        MojoExecutionException

  {
    try
    {
      AnnotationDB db = new AnnotationDB();
      File dir;

      dir = new File(outputDirectory);
      if (dir.exists())
      {
        getLog().info("Scanning directory " + dir.getAbsolutePath() + " for annotated classes...");
        URL dirUrl = dir.toURI().toURL();
        db.scanArchives(dirUrl);
      }

      if (scanTestClasses)
      {
        dir = new File(testOutputDirectory);
        if (dir.exists())
        {
          getLog().info("Scanning directory " + dir.getAbsolutePath() + " for annotated classes...");
          URL dirUrl = dir.toURI().toURL();
          db.scanArchives(dirUrl);
        }
      }

      if (scanDependencies != null)
      {
        Matcher matcher = SPLIT.matcher(scanDependencies);
        while (matcher.find())
        {
          getLog().info("Scanning dependencies for scope " + matcher.group());
          for (Artifact artifact : project.getDependencyArtifacts())
          {
            if (!artifact.getScope().equalsIgnoreCase(matcher.group()))
              continue;
            if (artifact.getFile() == null)
            {
              getLog().warn("Cannot scan dependency " + artifact.getId() + ": no JAR-file available!");
              continue;
            }
            getLog().info("Scanning dependency " + artifact.getId() + " for annotated classes...");
            db.scanArchives(artifact.getFile().toURI().toURL());
          }
        }
      }

      Set<String> classes = new HashSet<String>();
      if (db.getAnnotationIndex().containsKey(Entity.class.getName()))
        classes.addAll(db.getAnnotationIndex().get(Entity.class.getName()));
      if (db.getAnnotationIndex().containsKey(MappedSuperclass.class.getName()))
        classes.addAll(db.getAnnotationIndex().get(MappedSuperclass.class.getName()));
      if (db.getAnnotationIndex().containsKey(Embeddable.class.getName()))
        classes.addAll(db.getAnnotationIndex().get(Embeddable.class.getName()));

      Set<String> packages = new HashSet<String>();

      for (String name : classes)
      {
        Class<?> annotatedClass = classLoaderService.classForName(name);
        String packageName = annotatedClass.getPackage().getName();
        if (!packages.contains(packageName))
        {
          InputStream is =
              annotatedClass.getResourceAsStream("package-info.class");
          if (is == null)
          {
            // No compiled package-info available: no package-level annotations!
            getLog().debug("Package " + packageName + " is not annotated.");
          }
          else
          {
            if (tracker.check(packageName, is))
              getLog().debug("New or modified package: " + packageName);
            else
              getLog().debug("Unchanged package: " + packageName);
            getLog().info("Adding annotated package " + packageName);
            sources.addPackage(packageName);
          }
          packages.add(packageName);
        }
        String resourceName = annotatedClass.getName();
        resourceName =
            resourceName.substring(
                resourceName.lastIndexOf(".") + 1,
                resourceName.length()
                ) + ".class";
        InputStream is =
            annotatedClass
                .getResourceAsStream(resourceName);
        if (tracker.check(name, is))
          getLog().debug("New or modified class: " + name);
        else
          getLog().debug("Unchanged class: " + name);
        getLog().info("Adding annotated class " + annotatedClass);
        sources.addAnnotatedClass(annotatedClass);
      }
    }
    catch (Exception e)
    {
      getLog().error("Error while scanning!", e);
      throw new MojoFailureException(e.getMessage());
    }
  }

  private Properties loadPersistenceUnit(
      ClassLoaderService classLoaderService,
      Properties properties
      )
      throws
        MojoFailureException
  {
    PersistenceXmlParser parser =
        new PersistenceXmlParser(
            classLoaderService,
            PersistenceUnitTransactionType.RESOURCE_LOCAL
             );

    List<ParsedPersistenceXmlDescriptor> units = parser.doResolve(properties);

    if (persistenceUnit == null)
    {
      switch (units.size())
      {
        case 0:
          getLog().info("Found no META-INF/persistence.xml.");
          return new Properties();
        case 1:
          getLog().info("Using persistence-unit " + units.get(0).getName());
          return units.get(0).getProperties();
        default:
          StringBuilder builder = new StringBuilder();
          builder.append("No name provided and multiple persistence units found: ");
          Iterator<ParsedPersistenceXmlDescriptor> it = units.iterator();
          builder.append(it.next().getName());
          while (it.hasNext())
          {
            builder.append(", ");
            builder.append(it.next().getName());
          }
          builder.append('.');
          throw new MojoFailureException(builder.toString());
      }
    }

    for (ParsedPersistenceXmlDescriptor unit : units)
    {
      getLog().debug("Found persistence-unit " + unit.getName());
      if (!unit.getName().equals(persistenceUnit))
        continue;

      // See if we (Hibernate) are the persistence provider
      if (!ProviderChecker.isProvider(unit, properties))
      {
        getLog().debug("Wrong provider: " + unit.getProviderClassName());
        continue;
      }

      getLog().info("Using persistence-unit " + unit.getName());
      return unit.getProperties();
    }

    throw new MojoFailureException("Could not find persistence-unit " + persistenceUnit);
  }
}
