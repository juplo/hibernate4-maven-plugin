package de.juplo.plugins.hibernate;


import com.pyx4j.log4j.MavenLogAppender;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.cfgxml.spi.MappingReference;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.FORMAT_SQL;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_DELIMITER;
import static org.hibernate.cfg.AvailableSettings.HBM2DLL_CREATE_NAMESPACES;
import static org.hibernate.cfg.AvailableSettings.IMPLICIT_NAMING_STRATEGY;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_DRIVER;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_PASSWORD;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_URL;
import static org.hibernate.cfg.AvailableSettings.JPA_JDBC_USER;
import static org.hibernate.cfg.AvailableSettings.PASS;
import static org.hibernate.cfg.AvailableSettings.PHYSICAL_NAMING_STRATEGY;
import static org.hibernate.cfg.AvailableSettings.SHOW_SQL;
import static org.hibernate.cfg.AvailableSettings.USER;
import static org.hibernate.cfg.AvailableSettings.URL;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerCollectingImpl;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToFile;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;
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
  public final static String EXECUTE = "hibernate.schema.execute";
  public final static String OUTPUTDIRECTORY = "project.build.outputDirectory";
  public final static String SCAN_CLASSES = "hibernate.schema.scan.classes";
  public final static String SCAN_DEPENDENCIES = "hibernate.schema.scan.dependencies";
  public final static String SCAN_TESTCLASSES = "hibernate.schema.scan.test_classes";
  public final static String TEST_OUTPUTDIRECTORY = "project.build.testOutputDirectory";
  public final static String SKIPPED = "hibernate.schema.skipped";
  public final static String SCRIPT = "hibernate.schema.script";

  private final static Pattern SPLIT = Pattern.compile("[^,\\s]+");

  private final Set<String> packages = new HashSet<String>();


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
  private String buildDirectory;


  /** Parameters to configure the genaration of the SQL *********************/

  /**
   * Excecute the generated SQL.
   * If set to <code>false</code>, only the SQL-script is created and the
   * database is not touched.
   * <p>
   * <strong>Important:</strong>
   * This configuration value can only be configured through the
   * <code>pom.xml</code>, or by the definition of a system-property, because
   * it is not known by Hibernate nor JPA and, hence, not picked up from
   * their configuration!
   *
   * @parameter property="hibernate.schema.execute" default-value="true"
   * @since 2.0
   */
  private Boolean execute;

  /**
   * Skip execution
   * <p>
   * If set to <code>true</code>, the execution is skipped.
   * <p>
   * A skipped execution is signaled via the maven-property
   * <code>${hibernate.schema.skipped}</code>.
   * <p>
   * The execution is skipped automatically, if no modified or newly added
   * annotated classes are found and the dialect was not changed.
   * <p>
   * <strong>Important:</strong>
   * This configuration value can only be configured through the
   * <code>pom.xml</code>, or by the definition of a system-property, because
   * it is not known by Hibernate nor JPA and, hence, not picked up from
   * their configuration!
   *
   * @parameter property="hibernate.schema.skip" default-value="${maven.test.skip}"
   * @since 1.0
   */
  private boolean skip;

  /**
   * Force generation/execution
   * <p>
   * Force the generation and (if configured) the execution of the SQL, even if
   * no modified or newly added annotated classes where found and the
   * configuration was not changed.
   * <p>
   * <code>skip</code> takes precedence over <code>force</code>.
   * <p>
   * <strong>Important:</strong>
   * This configuration value can only be configured through the
   * <code>pom.xml</code>, or by the definition of a system-property, because
   * it is not known by Hibernate nor JPA and, hence, not picked up from
   * their configuration!
   *
   * @parameter property="hibernate.schema.force" default-value="false"
   * @since 1.0
   */
  private boolean force;

  /**
   * Hibernate dialect.
   *
   * @parameter property="hibernate.dialect"
   * @since 1.0
   */
  private String dialect;

  /**
   * Delimiter in output-file.
   * <p>
   * <strong>Important:</strong>
   * This configuration value can only be configured through the
   * <code>pom.xml</code>, or by the definition of a system-property, because
   * it is not known by Hibernate nor JPA and, hence, not picked up from
   * their configuration!
   *
   * @parameter property="hibernate.hbm2ddl.delimiter" default-value=";"
   * @since 1.0
   */
  private String delimiter;

  /**
   * Show the generated SQL in the command-line output.
   *
   * @parameter property="hibernate.show_sql"
   * @since 1.0
   */
  private Boolean show;

  /**
   * Format output-file.
   *
   * @parameter property="hibernate.format_sql"
   * @since 1.0
   */
  private Boolean format;

  /**
   * Specifies whether to automatically create also the database schema/catalog.
   *
   * @parameter property="hibernate.hbm2dll.create_namespaces" default-value="false"
   * @since 2.0
   */
  private Boolean createNamespaces;

  /**
   * Implicit naming strategy
   *
   * @parameter property="hibernate.implicit_naming_strategy"
   * @since 2.0
   */
  private String implicitNamingStrategy;

  /**
   * Physical naming strategy
   *
   * @parameter property="hibernate.physical_naming_strategy"
   * @since 2.0
   */
  private String physicalNamingStrategy;

  /**
   * Wether the project should be scanned for annotated-classes, or not
   * <p>
   * This parameter is intended to allow overwriting of the parameter
   * <code>exclude-unlisted-classes</code> of a <code>persistence-unit</code>.
   * If not specified, it defaults to <code>true</code>
   *
   * @parameter property="hibernate.schema.scan.classes"
   * @since 2.0
   */
  private Boolean scanClasses;

  /**
   * Classes-Directory to scan.
   * <p>
   * This parameter defaults to the maven build-output-directory for classes.
   * Additionally, all dependencies are scanned for annotated classes.
   * <p>
   * <strong>Important:</strong>
   * This configuration value can only be configured through the
   * <code>pom.xml</code>, or by the definition of a system-property, because
   * it is not known by Hibernate nor JPA and, hence, not picked up from
   * their configuration!
   *
   * @parameter property="project.build.outputDirectory"
   * @since 1.0
   */
  private String outputDirectory;

  /**
   * Dependency-Scopes, that should be scanned for annotated classes.
   * <p>
   * By default, only dependencies in the scope <code>compile</code> are
   * scanned for annotated classes. Multiple scopes can be seperated by
   * white space or commas.
   * <p>
   * If you do not want any dependencies to be scanned for annotated
   * classes, set this parameter to <code>none</code>.
   * <p>
   * The plugin does not scan for annotated classes in transitive
   * dependencies. If some of your annotated classes are hidden in a
   * transitive dependency, you can simply add that dependency explicitly.
   *
   * @parameter property="hibernate.schema.scan.dependencies" default-value="compile"
   * @since 1.0.3
   */
  private String scanDependencies;

  /**
   * Whether to scan the test-branch of the project for annotated classes, or
   * not.
   * <p>
   * If this parameter is set to <code>true</code> the test-classes of the
   * artifact will be scanned for hibernate-annotated classes additionally.
   * <p>
   * <strong>Important:</strong>
   * This configuration value can only be configured through the
   * <code>pom.xml</code>, or by the definition of a system-property, because
   * it is not known by Hibernate nor JPA and, hence, not picked up from
   * their configuration!
   *
   * @parameter property="hibernate.schema.scan.test_classes" default-value="false"
   * @since 1.0.1
   */
  private Boolean scanTestClasses;

  /**
   * Test-Classes-Directory to scan.
   * <p>
   * This parameter defaults to the maven build-output-directory for
   * test-classes.
   * <p>
   * This parameter is only used, when <code>scanTestClasses</code> is set
   * to <code>true</code>!
   * <p>
   * <strong>Important:</strong>
   * This configuration value can only be configured through the
   * <code>pom.xml</code>, or by the definition of a system-property, because
   * it is not known by Hibernate nor JPA and, hence, not picked up from
   * their configuration!
   *
   * @parameter property="project.build.testOutputDirectory"
   * @since 1.0.2
   */
  private String testOutputDirectory;


  /** Conection parameters *************************************************/

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


  /** Parameters to locate configuration sources ****************************/

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



  public final void execute(String filename)
    throws
      MojoFailureException,
      MojoExecutionException
  {
    if (skip)
    {
      getLog().info("Execution of hibernate-maven-plugin was skipped!");
      project.getProperties().setProperty(SKIPPED, "true");
      return;
    }

    ModificationTracker tracker;
    try
    {
      tracker = new ModificationTracker(buildDirectory, filename, getLog());
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new MojoFailureException("Digest-Algorithm MD5 is missing!", e);
    }

    final SimpleConnectionProvider connectionProvider =
        new SimpleConnectionProvider(getLog());

    try
    {
      /** Start extended logging */
      MavenLogAppender.startPluginLog(this);

      /** Load checksums for old mapping and configuration */
      tracker.load();

      /** Create the ClassLoader */
      MutableClassLoader classLoader = createClassLoader();

      /** Create a BootstrapServiceRegistry with the created ClassLoader */
      BootstrapServiceRegistry bootstrapServiceRegitry =
          new BootstrapServiceRegistryBuilder()
              .applyClassLoader(classLoader)
              .build();
      ClassLoaderService classLoaderService =
          bootstrapServiceRegitry.getService(ClassLoaderService.class);

      Properties properties = new Properties();
      ConfigLoader configLoader = new ConfigLoader(bootstrapServiceRegitry);

      /** Loading and merging configuration */
      properties.putAll(loadProperties(configLoader));
      LoadedConfig config = loadConfig(configLoader);
      if (config != null)
        properties.putAll(config.getConfigurationValues());
      ParsedPersistenceXmlDescriptor unit =
          loadPersistenceUnit(classLoaderService, properties);
      if (unit != null)
        properties.putAll(unit.getProperties());

      /** Overwriting/Completing configuration */
      configure(properties, tracker);

      /** Check configuration for modifications */
      if(tracker.track(properties))
        getLog().debug("Configuration has changed.");
      else
        getLog().debug("Configuration unchanged.");

      /** Check, that the outputfile is writable */
      final File output = getOutputFile(filename);
      /** Check, if the outputfile is missing or was changed */
      checkOutputFile(output, tracker);

      /** Configure Hibernate */
      final StandardServiceRegistry serviceRegistry =
          new StandardServiceRegistryBuilder(bootstrapServiceRegitry)
              .applySettings(properties)
              .addService(ConnectionProvider.class, connectionProvider)
              .build();
      final MetadataSources sources = new MetadataSources(serviceRegistry);

      /** Add the remaining class-path-elements */
      completeClassPath(classLoader);

      /** Apply mappings from hibernate-configuration, if present */
      if (config != null)
      {
        for (MappingReference mapping : config.getMappingReferences())
          mapping.apply(sources);
      }

      Set<String> classes;
      if (unit == null)
      {
        /** No persistent unit: default behaviour */
        if (scanClasses == null)
          scanClasses = true;
        Set<URL> urls = new HashSet<URL>();
        if (scanClasses)
          addRoot(urls, outputDirectory);
        if (scanTestClasses)
          addRoot(urls, testOutputDirectory);
        addDependencies(urls);
        classes = scanUrls(urls);
      }
      else
      {
        /** Follow configuration in persisten unit */
        if (scanClasses == null)
          scanClasses = !unit.isExcludeUnlistedClasses();
        Set<URL> urls = new HashSet<URL>();
        if (scanClasses)
        {
          /**
           * Scan the root of the persiten unit and configured jars for
           * annotated classes
           */
          urls.add(unit.getPersistenceUnitRootUrl());
          for (URL url : unit.getJarFileUrls())
            urls.add(url);
        }
        if (scanTestClasses)
          addRoot(urls, testOutputDirectory);
        classes = scanUrls(urls);
        for (String className : unit.getManagedClassNames())
          classes.add(className);
        /**
         * Add mappings from the default mapping-file
         * <code>META-INF/orm.xml</code>, if present
         */
        boolean error = false;
        InputStream is;
        is = classLoader.getResourceAsStream("META-INF/orm.xml");
        if (is != null)
        {
          getLog().info("Adding default JPA-XML-mapping from META-INF/orm.xml");
          try
          {
            tracker.track("META-INF/orm.xml", is);
            sources.addResource("META-INF/orm.xml");
          }
          catch (IOException e)
          {
            getLog().error("cannot read META-INF/orm.xml: " + e);
            error = true;
          }
        }
        else
        {
          getLog().debug("no META-INF/orm.xml found");
        }
        /**
         * Add mappings from files, that are explicitly configured in the
         * persistence unit
         */
        for (String mapping : unit.getMappingFileNames())
        {
          getLog().info("Adding explicitly configured mapping from " + mapping);
          is = classLoader.getResourceAsStream(mapping);
          if (is != null)
          {
            try
            {
              tracker.track(mapping, is);
              sources.addResource(mapping);
            }
            catch (IOException e)
            {
              getLog().info("cannot read mapping-file " + mapping + ": " + e);
              error = true;
            }
          }
          else
          {
            getLog().error("cannot find mapping-file " + mapping);
            error = true;
          }
        }
        if (error)
          throw new MojoFailureException(
              "error, while reading mappings configured in persistence-unit \"" +
              unit.getName() +
              "\""
              );
      }

      /** Add the configured/collected annotated classes */
      for (String className : classes)
        addAnnotated(className, sources, classLoaderService, tracker);

      /** Add explicitly configured classes */
      addMappings(sources, tracker);

      /** Skip execution, if mapping and configuration is unchanged */
      if (!tracker.modified())
      {
        getLog().info("Mapping and configuration unchanged.");
        if (force)
          getLog().info("Generation/execution is forced!");
        else
        {
          getLog().info("Skipping schema generation!");
          project.getProperties().setProperty(SKIPPED, "true");
          return;
        }
      }


      /** Truncate output file */
      try
      {
        new FileOutputStream(output).getChannel().truncate(0).close();
      }
      catch (IOException e)
      {
        String error =
            "Error while truncating " + output.getAbsolutePath() + ": "
            + e.getMessage();
        getLog().warn(error);
        throw new MojoExecutionException(error);
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

      /** Prepare the generation of the SQL */
      Map settings = new HashMap();
      settings.putAll(
          serviceRegistry
              .getService(ConfigurationService.class)
              .getSettings()
              );
      ExceptionHandlerCollectingImpl handler =
          new ExceptionHandlerCollectingImpl();
      ExecutionOptions options =
          SchemaManagementToolCoordinator
              .buildExecutionOptions(settings, handler);
      final EnumSet<TargetType> targetTypes = EnumSet.of(TargetType.SCRIPT);
      if (execute)
        targetTypes.add(TargetType.DATABASE);
      TargetDescriptor target = new TargetDescriptor()
      {
        @Override
        public EnumSet<TargetType> getTargetTypes()
        {
          return targetTypes;
        }

        @Override
        public ScriptTargetOutput getScriptTargetOutput()
        {
          String charset =
              (String)
              serviceRegistry
                  .getService(ConfigurationService.class)
                  .getSettings()
                  .get(AvailableSettings.HBM2DDL_CHARSET_NAME);
          return new ScriptTargetOutputToFile(output, charset);
        }
      };

      /**
       * Change class-loader of current thread.
       * This is necessary, because still not all parts of Hibernate 5 use
       * the newly introduced ClassLoaderService and will fail otherwise!
       */
      Thread thread = Thread.currentThread();
      ClassLoader contextClassLoader = thread.getContextClassLoader();
      try
      {
        thread.setContextClassLoader(classLoader);
        build((MetadataImplementor)metadataBuilder.build(), options, target);
      }
      finally
      {
        thread.setContextClassLoader(contextClassLoader);
        for (Exception e : handler.getExceptions())
          getLog().error(e.getMessage());
        /** Track, the content of the generated script */
        checkOutputFile(output, tracker);
      }
    }
    catch (MojoExecutionException e)
    {
      tracker.failed();
      throw e;
    }
    catch (MojoFailureException e)
    {
      tracker.failed();
      throw e;
    }
    catch (RuntimeException e)
    {
      tracker.failed();
      throw e;
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


  abstract void build(
      MetadataImplementor metadata,
      ExecutionOptions options,
      TargetDescriptor target
      )
    throws
      MojoFailureException,
      MojoExecutionException;


  private MutableClassLoader createClassLoader() throws MojoExecutionException
  {
    try
    {
      getLog().debug("Creating ClassLoader for project-dependencies...");
      LinkedHashSet<URL> urls = new LinkedHashSet<URL>();
      File file;

      file = new File(testOutputDirectory);
      if (!file.exists())
      {
        getLog().info("Creating test-output-directory: " + testOutputDirectory);
        file.mkdirs();
      }
      urls.add(file.toURI().toURL());

      file = new File(outputDirectory);
      if (!file.exists())
      {
        getLog().info("Creating output-directory: " + outputDirectory);
        file.mkdirs();
      }
      urls.add(file.toURI().toURL());

      return new MutableClassLoader(urls, getLog());
    }
    catch (Exception e)
    {
      getLog().error("Error while creating ClassLoader!", e);
      throw new MojoExecutionException(e.getMessage());
    }
  }

  private void completeClassPath(MutableClassLoader classLoader)
      throws
        MojoExecutionException
  {
    try
    {
      getLog().debug("Completing class-paths of the ClassLoader for project-dependencies...");
      List<String> classpathFiles = project.getCompileClasspathElements();
      if (scanTestClasses)
        classpathFiles.addAll(project.getTestClasspathElements());
      LinkedHashSet<URL> urls = new LinkedHashSet<URL>();
      for (String pathElement : classpathFiles)
      {
        getLog().debug("Dependency: " + pathElement);
        urls.add(new File(pathElement).toURI().toURL());
      }
      classLoader.add(urls);
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

  private LoadedConfig loadConfig(ConfigLoader configLoader)
      throws MojoExecutionException
  {
    /** Try to read configuration from configuration-file */
    if (hibernateConfig == null)
    {
      try
      {
        return configLoader.loadConfigXmlResource("hibernate.cfg.xml");
      }
      catch (ConfigurationException e)
      {
        getLog().debug(e.getMessage());
        return null;
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
          return configLoader.loadConfigXmlFile(file);
        }
        else
        {
          return configLoader.loadConfigXmlResource(hibernateConfig);
        }
      }
      catch (ConfigurationException e)
      {
        getLog().error("Error while reading configuration!", e);
        throw new MojoExecutionException(e.getMessage());
      }
    }
  }

  private void configure(Properties properties, ModificationTracker tracker)
      throws MojoFailureException
  {
    /**
     * Special treatment for the configuration-value "execute": if it is
     * switched to "true", the genearation fo the schema should be forced!
     */
    if (tracker.check(EXECUTE, execute.toString()) && execute)
    {
      getLog().info(
          "hibernate.schema.execute was switched on: " +
          "forcing generation/execution of SQL"
          );
      tracker.touch();
    }
    configure(properties, execute, EXECUTE);

    /**
     * Configure the generation of the SQL.
     * Overwrite values from properties-file if the configuration parameter is
     * known to Hibernate.
     */
    configure(properties, dialect, DIALECT);
    configure(properties, delimiter, HBM2DDL_DELIMITER);
    configure(properties, format, FORMAT_SQL);
    configure(properties, createNamespaces, HBM2DLL_CREATE_NAMESPACES);
    configure(properties, implicitNamingStrategy, IMPLICIT_NAMING_STRATEGY);
    configure(properties, physicalNamingStrategy, PHYSICAL_NAMING_STRATEGY);
    configure(properties, outputDirectory, OUTPUTDIRECTORY);
    configure(properties, scanDependencies, SCAN_DEPENDENCIES);
    configure(properties, scanTestClasses, SCAN_TESTCLASSES);
    configure(properties, testOutputDirectory, TEST_OUTPUTDIRECTORY);

    /**
     * Special treatment for the configuration-value "show": a change of its
     * configured value should not lead to a regeneration of the database
     * schama!
     */
    if (show == null)
      show = Boolean.valueOf(properties.getProperty(SHOW_SQL));
    else
      properties.setProperty(SHOW_SQL, show.toString());

    /**
     * Configure the connection parameters.
     * Overwrite values from properties-file.
     */
    configure(properties, driver, DRIVER, JPA_JDBC_DRIVER);
    configure(properties, url, URL, JPA_JDBC_URL);
    configure(properties, username, USER, JPA_JDBC_USER);
    configure(properties, password, PASS, JPA_JDBC_PASSWORD);

    if (properties.isEmpty())
    {
      getLog().error("No properties set!");
      throw new MojoFailureException("Hibernate configuration is missing!");
    }

    getLog().info("Gathered configuration:");
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

    if (properties.containsKey(alternativeKey))
    {
      if (properties.containsKey(key))
      {
        getLog().warn(
            "Ignoring property " + alternativeKey + "=\"" +
            properties.getProperty(alternativeKey) +
            "\" in favour for property " + key + "=\"" +
            properties.getProperty(key) + "\""
            );
        properties.remove(alternativeKey);
      }
      else
      {
        value = properties.getProperty(alternativeKey);
        properties.remove(alternativeKey);
        getLog().info(
            "Using value \"" + value + "\" from property " + alternativeKey +
            " for property " + key
            );
        properties.setProperty(key, value);
      }
    }
  }

  private void configure(Properties properties, String value, String key)
  {
    if (value != null)
    {
      if (properties.containsKey(key))
      {
        if (!properties.getProperty(key).equals(value))
        {
          getLog().info(
              "Overwriting property " + key + "=\"" +
              properties.getProperty(key) +
              "\" with value \"" + value + "\""
              );
          properties.setProperty(key, value);
        }
      }
      else
      {
        getLog().debug("Using value \"" + value + "\" for property " + key);
        properties.setProperty(key, value);
      }
    }
  }

  private void configure(Properties properties, Boolean value, String key)
  {
    configure(properties, value == null ? null : value.toString(), key);
  }

  private File getOutputFile(String filename)
      throws
        MojoExecutionException
  {
    File output = new File(filename);

    if (!output.isAbsolute())
    {
      // Interpret relative file path relative to build directory
      output = new File(buildDirectory, filename);
    }
    getLog().debug("Output file: " + output.getPath());

    // Ensure that directory path for specified file exists
    File outFileParentDir = output.getParentFile();
    if (null != outFileParentDir && !outFileParentDir.exists())
    {
      try
      {
        getLog().info(
            "Creating directory path for output file:" +
            outFileParentDir.getPath()
            );
        outFileParentDir.mkdirs();
      }
      catch (Exception e)
      {
        String error =
            "Error creating directory path for output file: " + e.getMessage();
        getLog().error(error);
        throw new MojoExecutionException(error);
      }
    }

    try
    {
      output.createNewFile();
    }
    catch (IOException e)
    {
      String error = "Error creating output file: " + e.getMessage();
      getLog().error(error);
      throw new MojoExecutionException(error);
    }

    if (!output.canWrite())
    {
      String error =
          "Output file " + output.getAbsolutePath() + " is not writable!";
      getLog().error(error);
      throw new MojoExecutionException(error);
    }

    return output;
  }

  private void checkOutputFile(File output, ModificationTracker tracker)
      throws
        MojoExecutionException
  {
    try
    {
      if (output.exists())
        tracker.track(SCRIPT, new FileInputStream(output));
      else
        tracker.track(SCRIPT, ZonedDateTime.now().toString());
    }
    catch (IOException e)
    {
      String error =
          "Error while checking the generated script: " + e.getMessage();
      getLog().error(error);
      throw new MojoExecutionException(error);
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
            if (tracker.track(filename, new FileInputStream(file)))
              getLog().debug("Found new or modified mapping-file: " + filename);
            else
              getLog().debug("Mapping-file unchanged: " + filename);

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

  private void addRoot(Set<URL> urls, String path) throws MojoFailureException
  {
    try
    {
      File dir = new File(outputDirectory);
      if (dir.exists())
      {
        getLog().info("Adding " + dir.getAbsolutePath() + " to the list of roots to scan...");
        urls.add(dir.toURI().toURL());
      }
    }
    catch (MalformedURLException e)
    {
      getLog().error("error while adding the project-root to the list of roots to scan!", e);
      throw new MojoFailureException(e.getMessage());
    }
  }

  private void addDependencies(Set<URL> urls) throws MojoFailureException
  {
    try
    {
      if (scanDependencies != null)
      {
        Matcher matcher = SPLIT.matcher(scanDependencies);
        while (matcher.find())
        {
          getLog().info("Adding dependencies from scope " + matcher.group() + " to the list of roots to scan");
          for (Artifact artifact : project.getDependencyArtifacts())
          {
            if (!artifact.getScope().equalsIgnoreCase(matcher.group()))
              continue;
            if (artifact.getFile() == null)
            {
              getLog().warn("Cannot add dependency " + artifact.getId() + ": no JAR-file available!");
              continue;
            }
            getLog().info("Adding dependencies from scope " + artifact.getId() + " to the list of roots to scan");
            urls.add(artifact.getFile().toURI().toURL());
          }
        }
      }
    }
    catch (MalformedURLException e)
    {
      getLog().error("Error while adding dependencies to the list of roots to scan!", e);
      throw new MojoFailureException(e.getMessage());
    }
  }

  private Set<String> scanUrls(Set<URL> scanRoots)
      throws
        MojoFailureException
  {
    try
    {
      AnnotationDB db = new AnnotationDB();
      for (URL root : scanRoots)
        db.scanArchives(root);

      Set<String> classes = new HashSet<String>();
      if (db.getAnnotationIndex().containsKey(Entity.class.getName()))
        classes.addAll(db.getAnnotationIndex().get(Entity.class.getName()));
      if (db.getAnnotationIndex().containsKey(MappedSuperclass.class.getName()))
        classes.addAll(db.getAnnotationIndex().get(MappedSuperclass.class.getName()));
      if (db.getAnnotationIndex().containsKey(Embeddable.class.getName()))
        classes.addAll(db.getAnnotationIndex().get(Embeddable.class.getName()));

      return classes;
    }
    catch (Exception e)
    {
      getLog().error("Error while scanning!", e);
      throw new MojoFailureException(e.getMessage());
    }
  }

  private void addAnnotated(
      String name,
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
      getLog().info("Adding annotated resource: " + name);
      String packageName = null;

      boolean error = false;
      try
      {
        Class<?> annotatedClass = classLoaderService.classForName(name);
        String resourceName = annotatedClass.getName();
        resourceName =
            resourceName.substring(
                resourceName.lastIndexOf(".") + 1,
                resourceName.length()
                ) + ".class";
        InputStream is = annotatedClass.getResourceAsStream(resourceName);
        if (is != null)
        {
          if (tracker.track(name, is))
            getLog().debug("New or modified class: " + name);
          else
            getLog().debug("Unchanged class: " + name);
          sources.addAnnotatedClass(annotatedClass);
          packageName = annotatedClass.getPackage().getName();
        }
        else
        {
          getLog().error("cannot find ressource " + resourceName + " for class " + name);
          error = true;
        }
      }
      catch(ClassLoadingException e)
      {
        packageName = name;
      }
      if (error)
      {
        throw new MojoExecutionException("error while inspecting annotated class " + name);
      }

      while (packageName != null)
      {
        if (packages.contains(packageName))
          return;
        String resource = packageName.replace('.', '/') + "/package-info.class";
        InputStream is = classLoaderService.locateResourceStream(resource);
        if (is == null)
        {
          // No compiled package-info available: no package-level annotations!
          getLog().debug("Package " + packageName + " is not annotated.");
        }
        else
        {
          if (tracker.track(packageName, is))
            getLog().debug("New or modified package: " + packageName);
          else
           getLog().debug("Unchanged package: " + packageName);
          getLog().info("Adding annotations from package " + packageName);
          sources.addPackage(packageName);
        }
        packages.add(packageName);
        int i = packageName.lastIndexOf('.');
        if (i < 0)
          packageName = null;
        else
          packageName = packageName.substring(0,i);
      }
    }
    catch (Exception e)
    {
      getLog().error("Error while adding the annotated class " + name, e);
      throw new MojoFailureException(e.getMessage());
    }
  }

  private ParsedPersistenceXmlDescriptor loadPersistenceUnit(
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

    Map<String, ParsedPersistenceXmlDescriptor> units =
        parser.doResolve(properties);

    if (persistenceUnit == null)
    {
      Iterator<String> names = units.keySet().iterator();
      if (!names.hasNext())
      {
        getLog().info("Found no META-INF/persistence.xml.");
        return null;
      }

      String name = names.next();
      if (!names.hasNext())
      {
          getLog().info("Using persistence-unit " + name);
          return units.get(name);
      }

      StringBuilder builder = new StringBuilder();
      builder.append("No name provided and multiple persistence units found: ");
      builder.append(name);
      while(names.hasNext())
      {
        builder.append(", ");
        builder.append(names.next());
      }
      builder.append('.');
      throw new MojoFailureException(builder.toString());
    }

    if (units.containsKey(persistenceUnit))
    {
      getLog().info("Using configured persistence-unit " + persistenceUnit);
      return units.get(persistenceUnit);
    }

    throw new MojoFailureException("Could not find persistence-unit " + persistenceUnit);
  }
}
