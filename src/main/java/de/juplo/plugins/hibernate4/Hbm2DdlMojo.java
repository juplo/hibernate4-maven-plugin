package de.juplo.plugins.hibernate4;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.pyx4j.log4j.MavenLogAppender;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
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
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.ProviderChecker;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaExport.Type;
import org.hibernate.tool.hbm2ddl.Target;
import org.scannotation.AnnotationDB;


/**
 * Goal which extracts the hibernate-mapping-configuration and
 * exports an according SQL-database-schema.
 *
 * @goal export
 * @phase process-classes
 * @threadSafe
 * @requiresDependencyResolution runtime
 */
public class Hbm2DdlMojo extends AbstractMojo
{
  public final static String EXPORT_SKIPPED_PROPERTY = "hibernate.export.skipped";

  public final static String DRIVER_CLASS = "hibernate.connection.driver_class";
  public final static String URL = "hibernate.connection.url";
  public final static String USERNAME = "hibernate.connection.username";
  public final static String PASSWORD = "hibernate.connection.password";
  public final static String DIALECT = "hibernate.dialect";
  public final static String NAMING_STRATEGY="hibernate.ejb.naming_strategy";
  public final static String ENVERS = "hibernate.export.envers";

  public final static String MD5S = "hibernate4-generatedschema.md5s";

  private final static Pattern split = Pattern.compile("[^,\\s]+");


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
   * <p>
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
  private String driverClassName;

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
  private String hibernateDialect;

  /**
   * Hibernate Naming Strategy
   *
   * @parameter property="hibernate.ejb.naming_strategy"
   * @since 1.0.2
   */
  private String hibernateNamingStrategy;

  /**
   * Path to Hibernate properties file.
   * If this parameter is not set the plugin will try to load the configuration
   * from a file <code>hibernate.properties</code> on the classpath. The
   * test-classpath takes precedence.
   *
   * @parameter
   * @since 1.0
   */
  private String hibernateProperties;

  /**
   * Path to Hibernate configuration file (.cfg.xml).
   * Settings in this file will overwrite settings in the properties file.
   * If this parameter is not set the plugin will try to load the configuration
   * from a file <code>hibernate.cfg.xml</code> on the classpath. The
   * test-classpath takes precedence.
   *
   * @parameter
   * @since 1.1.0
   */
  private String hibernateConfig;

  /**
   * Name of the persistence-unit.
   * If there is only one persistence-unit available, that unit will be used
   * automatically.
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
  private String hibernateMapping;

  /**
   * Target of execution:
   * <ul>
   *   <li><strong>NONE</strong> only export schema to SQL-script (forces execution, signals skip)</li>
   *   <li><strong>EXPORT</strong> create database (<strong>DEFAULT!</strong>). forces execution, signals skip)</li>
   *   <li><strong>SCRIPT</strong> export schema to SQL-script and print it to STDOUT</li>
   *   <li><strong>BOTH</strong></li>
   * </ul>
   *
   * A database connection is only needed for EXPORT and BOTH, but a
   * Hibernate-Dialect must always be chosen.
   *
   * @parameter property="hibernate.export.target" default-value="EXPORT"
   * @since 1.0
   */
  private String target;

  /**
   * Type of execution.
   * <ul>
   *   <li><strong>NONE</strong> do nothing - just validate the configuration</li>
   *   <li><strong>CREATE</strong> create database-schema</li>
   *   <li><strong>DROP</strong> drop database-schema</li>
   *   <li><strong>BOTH</strong> (<strong>DEFAULT!</strong>)</li>
   * </ul>
   *
   * If NONE is choosen, no databaseconnection is needed.
   *
   * @parameter property="hibernate.export.type" default-value="BOTH"
   * @since 1.0
   */
  private String type;

  /**
   * Output file.
   *
   * @parameter property="hibernate.export.schema.filename" default-value="${project.build.directory}/schema.sql"
   * @since 1.0
   */
  private String outputFile;

  /**
   * Delimiter in output-file.
   *
   * @parameter property="hibernate.export.schema.delimiter" default-value=";"
   * @since 1.0
   */
  private String delimiter;

  /**
   * Format output-file.
   *
   * @parameter property="hibernate.export.schema.format" default-value="true"
   * @since 1.0
   */
  private boolean format;

  /**
   * Generate envers schema for auditing tables.
   *
   * @parameter property="hibernate.export.envers" default-value="true"
   * @since 1.0.3
   */
  private boolean envers;


  @Override
  public void execute()
    throws
      MojoFailureException,
      MojoExecutionException
  {
    if (skip)
    {
      getLog().info("Execution of hibernate4-maven-plugin:export was skipped!");
      project.getProperties().setProperty(EXPORT_SKIPPED_PROPERTY, "true");
      return;
    }

    Map<String,String> md5s;
    boolean modified = false;
    File saved = new File(buildDirectory + File.separator + MD5S);

    if (saved.isFile() && saved.length() > 0)
    {
      try
      {
        FileInputStream fis = new FileInputStream(saved);
        ObjectInputStream ois = new ObjectInputStream(fis);
        md5s = (HashMap<String,String>)ois.readObject();
        ois.close();
      }
      catch (Exception e)
      {
        md5s = new HashMap<String,String>();
        getLog().warn("Cannot read timestamps from saved: " + e);
      }
    }
    else
    {
      md5s = new HashMap<String,String>();
      try
      {
        saved.createNewFile();
      }
      catch (IOException e)
      {
        getLog().debug("Cannot create file \"" + saved.getPath() + "\" for timestamps: " + e);
      }
    }

    URLClassLoader classLoader = null;
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
      classLoader =
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

    Set<Class<?>> classes =
        new TreeSet<Class<?>>(
            new Comparator<Class<?>>() {
              @Override
              public int compare(Class<?> a, Class<?> b)
              {
                return a.getName().compareTo(b.getName());
              }
            }
          );

    try
    {
      AnnotationDB db = new AnnotationDB();
      File dir = new File(outputDirectory);
      if (dir.exists())
      {
        getLog().info("Scanning directory " + outputDirectory + " for annotated classes...");
        URL dirUrl = dir.toURI().toURL();
        db.scanArchives(dirUrl);
      }
      if (scanTestClasses)
      {
        dir = new File(testOutputDirectory);
        if (dir.exists())
        {
          getLog().info("Scanning directory " + testOutputDirectory + " for annotated classes...");
          URL dirUrl = dir.toURI().toURL();
          db.scanArchives(dirUrl);
        }
      }
      if (scanDependencies != null)
      {
        Matcher matcher = split.matcher(scanDependencies);
        while (matcher.find())
        {
          getLog().info("Scanning dependencies for scope " + matcher.group());
          for (Artifact artifact : project.getDependencyArtifacts())
          {
            if (!artifact.getScope().equalsIgnoreCase(matcher.group()))
              continue;
            if (artifact.getFile() == null)
            {
              getLog().warn(
                  "Cannot scan dependency " +
                  artifact.getId() +
                  ": no JAR-file available!"
                  );
              continue;
            }
            getLog().info(
                "Scanning dependency " +
                artifact.getId() +
                " for annotated classes..."
                );
            db.scanArchives(artifact.getFile().toURI().toURL());
          }
        }
      }

      Set<String> classNames = new HashSet<String>();
      if (db.getAnnotationIndex().containsKey(Entity.class.getName()))
        classNames.addAll(db.getAnnotationIndex().get(Entity.class.getName()));
      if (db.getAnnotationIndex().containsKey(MappedSuperclass.class.getName()))
        classNames.addAll(db.getAnnotationIndex().get(MappedSuperclass.class.getName()));
      if (db.getAnnotationIndex().containsKey(Embeddable.class.getName()))
        classNames.addAll(db.getAnnotationIndex().get(Embeddable.class.getName()));

      MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
      for (String name : classNames)
      {
        Class<?> annotatedClass = classLoader.loadClass(name);
        classes.add(annotatedClass);
        String resourceName = annotatedClass.getName();
        resourceName = resourceName.substring(resourceName.lastIndexOf(".") + 1, resourceName.length()) + ".class";
        InputStream is =
            annotatedClass
                .getResourceAsStream(resourceName);
        byte[] buffer = new byte[1024*4]; // copy data in 4MB-chunks
        int i;
        while((i = is.read(buffer)) > -1)
          digest.update(buffer, 0, i);
        is.close();
        byte[] bytes = digest.digest();
        BigInteger bi = new BigInteger(1, bytes);
        String newMd5 = String.format("%0" + (bytes.length << 1) + "x", bi);
        String oldMd5 = !md5s.containsKey(name) ? "" : md5s.get(name);
        if (!newMd5.equals(oldMd5))
        {
          getLog().debug("Found new or modified annotated class: " + name);
          modified = true;
          md5s.put(name, newMd5);
        }
        else
        {
          getLog().debug(oldMd5 + " -> class unchanged: " + name);
        }
      }
    }
    catch (ClassNotFoundException e)
    {
      getLog().error("Error while adding annotated classes!", e);
      throw new MojoExecutionException(e.getMessage());
    }
    catch (Exception e)
    {
      getLog().error("Error while scanning!", e);
      throw new MojoFailureException(e.getMessage());
    }


    ValidationConfiguration config = new ValidationConfiguration();
    // Clear unused system-properties
    config.setProperties(new Properties());


    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    StandardServiceRegistryImpl registry = null;
    MavenLogAppender.startPluginLog(this);

    try
    {
      /**
       * Change class-loader of current thread, so that hibernate can
       * see all dependencies!
       */
      Thread.currentThread().setContextClassLoader(classLoader);


      /** Try to read configuration from properties-file */
      try
      {
        if (hibernateProperties == null)
        {
          URL url = classLoader.findResource("hibernate.properties");
          if (url == null)
          {
            getLog().info("No hibernate.properties on the classpath!");
          }
          else
          {
            getLog().info("Reading settings from hibernate.properties on the classpath.");
            Properties properties = new Properties();
            properties.load(url.openStream());
            config.setProperties(properties);
          }
        }
        else
        {
          File file = new File(hibernateProperties);
          if (file.exists())
          {
            getLog().info("Reading settings from file " + hibernateProperties + "...");
            Properties properties = new Properties();
            properties.load(new FileInputStream(file));
            config.setProperties(properties);
          }
          else
            getLog().info("No hibernate-properties-file found! (Checked path: " + hibernateProperties + ")");
        }
      }
      catch (IOException e)
      {
        getLog().error("Error while reading properties!", e);
        throw new MojoExecutionException(e.getMessage());
      }

      /** Try to read configuration from configuration-file */
      try
      {
        if (hibernateConfig == null)
        {
          URL url = classLoader.findResource("hibernate.cfg.xml");
          if (url == null)
          {
            getLog().info("No hibernate.cfg.xml on the classpath!");
          }
          else
          {
            getLog().info("Reading settings from hibernate.cfg.xml on the classpath.");
            config.configure(url);
          }
        }
        else
        {
          File file = new File(hibernateConfig);
          if (file.exists())
          {
            getLog().info("Reading configuration from file " + hibernateConfig + "...");
            config.configure(file);
          }
          else
            getLog().info("No hibernate-configuration-file found! (Checked path: " + hibernateConfig + ")");
        }
      }
      catch (Exception e)
      {
        getLog().error("Error while reading configuration!", e);
        throw new MojoExecutionException(e.getMessage());
      }

      ParsedPersistenceXmlDescriptor persistenceUnitDescriptor =
          getPersistenceUnitDescriptor(
              persistenceUnit,
              config.getProperties(),
              new MavenProjectClassLoaderService(classLoader)
              );
      if (persistenceUnitDescriptor != null)
        config.setProperties(persistenceUnitDescriptor.getProperties());

      /** Overwrite values from properties-file or set, if given */
      if (driverClassName != null)
      {
        if (config.getProperties().containsKey(DRIVER_CLASS))
          getLog().debug(
              "Overwriting property " +
              DRIVER_CLASS + "=" + config.getProperty(DRIVER_CLASS) +
              " with the value " + driverClassName
            );
        else
          getLog().debug("Using the value " + driverClassName);
        config.setProperty(DRIVER_CLASS, driverClassName);
      }
      if (url != null)
      {
        if (config.getProperties().containsKey(URL))
          getLog().debug(
              "Overwriting property " +
              URL + "=" + config.getProperty(URL) +
              " with the value " + url
            );
        else
          getLog().debug("Using the value " + url);
        config.setProperty(URL, url);
      }
      if (username != null)
      {
        if (config.getProperties().containsKey(USERNAME))
          getLog().debug(
              "Overwriting property " +
              USERNAME + "=" + config.getProperty(USERNAME) +
              " with the value " + username
            );
        else
          getLog().debug("Using the value " + username);
        config.setProperty(USERNAME, username);
      }
      if (password != null)
      {
        if (config.getProperties().containsKey(PASSWORD))
          getLog().debug(
              "Overwriting property " +
              PASSWORD + "=" + config.getProperty(PASSWORD) +
              " with value " + password
            );
        else
          getLog().debug("Using value " + password + " for property " + PASSWORD);
        config.setProperty(PASSWORD, password);
      }
      if (hibernateDialect != null)
      {
        if (config.getProperties().containsKey(DIALECT))
          getLog().debug(
              "Overwriting property " +
              DIALECT + "=" + config.getProperty(DIALECT) +
              " with value " + hibernateDialect
            );
        else
          getLog().debug(
              "Using value " + hibernateDialect + " for property " + DIALECT
              );
        config.setProperty(DIALECT, hibernateDialect);
      }
      if ( hibernateNamingStrategy != null )
      {
        if ( config.getProperties().contains(NAMING_STRATEGY))
          getLog().debug(
              "Overwriting property " +
              NAMING_STRATEGY + "=" + config.getProperty(NAMING_STRATEGY) +
              " with value " + hibernateNamingStrategy
             );
        else
          getLog().debug(
              "Using value " + hibernateNamingStrategy + " for property " +
              NAMING_STRATEGY
              );
        config.setProperty(NAMING_STRATEGY, hibernateNamingStrategy);
      }

      /** The generated SQL varies with the dialect! */
      if (md5s.containsKey(DIALECT))
      {
        String dialect = config.getProperty(DIALECT);
        if (md5s.get(DIALECT).equals(dialect))
          getLog().debug("SQL-dialect unchanged.");
        else
        {
          modified = true;
          if (dialect == null)
          {
            getLog().debug("SQL-dialect was unset.");
            md5s.remove(DIALECT);
          }
          else
          {
            getLog().debug("SQL-dialect changed: " + dialect);
            md5s.put(DIALECT, dialect);
          }
        }
      }
      else
      {
        String dialect = config.getProperty(DIALECT);
        if (dialect != null)
        {
          modified = true;
          md5s.put(DIALECT, config.getProperty(DIALECT));
        }
      }

      /** The generated SQL varies with the envers-configuration */
      if (md5s.get(ENVERS) != null)
      {
        if (md5s.get(ENVERS).equals(Boolean.toString(envers)))
          getLog().debug("Envers-Configuration unchanged. Enabled: " + envers);
        else
        {
          getLog().debug("Envers-Configuration changed. Enabled: " + envers);
          modified = true;
          md5s.put(ENVERS, Boolean.toString(envers));
        }
      }
      else
      {
        modified = true;
        md5s.put(ENVERS, Boolean.toString(envers));
      }

      if (config.getProperties().isEmpty())
      {
        getLog().error("No properties set!");
        throw new MojoFailureException("Hibernate configuration is missing!");
      }

      getLog().info("Gathered hibernate-configuration (turn on debugging for details):");
      for (Entry<Object,Object> entry : config.getProperties().entrySet())
        getLog().info("  " + entry.getKey() + " = " + entry.getValue());


      getLog().debug("Adding explicitly configured mappings...");
      if (hibernateMapping != null)
      {
        try
        {
          MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
          for (String filename : hibernateMapping.split("[\\s,]+"))
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
            if (file != null && file.exists())
            {
              InputStream is = new FileInputStream(file);
              byte[] buffer = new byte[1024*4]; // copy data in 4MB-chunks
              int i;
              while((i = is.read(buffer)) > -1)
                digest.update(buffer, 0, i);
              is.close();
              byte[] bytes = digest.digest();
              BigInteger bi = new BigInteger(1, bytes);
              String newMd5 = String.format("%0" + (bytes.length << 1) + "x", bi);
              String oldMd5 = !md5s.containsKey(filename) ? "" : md5s.get(filename);
              if (!newMd5.equals(oldMd5))
              {
                getLog().debug("Found new or modified mapping-file: " + filename);
                modified = true;
                md5s.put(filename, newMd5);
              }
              else
              {
                getLog().debug(oldMd5 + " -> mapping-file unchanged: " + filename);
              }
              getLog().debug("Adding mappings from XML-configurationfile: " + file);
              config.addFile(file);
            }
            else
              throw new MojoFailureException("File " + filename + " could not be found in any of the configured resource-directories!");
          }
        }
        catch (NoSuchAlgorithmException e)
        {
          throw new MojoFailureException("Cannot calculate MD5 sums!", e);
        }
        catch (FileNotFoundException e)
        {
          throw new MojoFailureException("Cannot calculate MD5 sums!", e);
        }
        catch (IOException e)
        {
          throw new MojoFailureException("Cannot calculate MD5 sums!", e);
        }
      }

      getLog().debug("Adding annotated classes to hibernate-mapping-configuration...");
      // build annotated packages
      Set<String> packages = new HashSet<String>();
      for (Class<?> annotatedClass : classes)
      {
        String packageName = annotatedClass.getPackage().getName();
        if (!packages.contains(packageName))
        {
          getLog().debug("Add package " + packageName);
          packages.add(packageName);
          config.addPackage(packageName);
          getLog().debug("type definintions" + config.getTypeDefs());
        }
        getLog().debug("Class " + annotatedClass);
        config.addAnnotatedClass(annotatedClass);
      }

      Target target = null;
      try
      {
        target = Target.valueOf(this.target.toUpperCase());
      }
      catch (IllegalArgumentException e)
      {
        getLog().error("Invalid value for configuration-option \"target\": " + this.target);
        getLog().error("Valid values are: NONE, SCRIPT, EXPORT, BOTH");
        throw new MojoExecutionException("Invalid value for configuration-option \"target\"");
      }
      Type type = null;
      try
      {
        type = Type.valueOf(this.type.toUpperCase());
      }
      catch (IllegalArgumentException e)
      {
        getLog().error("Invalid value for configuration-option \"type\": " + this.type);
        getLog().error("Valid values are: NONE, CREATE, DROP, BOTH");
        throw new MojoExecutionException("Invalid value for configuration-option \"type\"");
      }


      if (config.getProperty(DIALECT) == null)
        throw new MojoFailureException("hibernate-dialect must be set!");


      if (target.equals(Target.SCRIPT) || target.equals(Target.NONE))
      {
        project.getProperties().setProperty(EXPORT_SKIPPED_PROPERTY, "true");
      }
      if (
          !modified
          && !target.equals(Target.SCRIPT)
          && !target.equals(Target.NONE)
          && !force
        )
      {
        getLog().info("No modified annotated classes or mapping-files found and dialect unchanged.");
        getLog().info("Skipping schema generation!");
        project.getProperties().setProperty(EXPORT_SKIPPED_PROPERTY, "true");
        return;
      }


      if ( config.getProperties().containsKey(NAMING_STRATEGY))
      {
        String namingStrategy = config.getProperty(NAMING_STRATEGY);
        getLog().debug("Explicitly set NamingStrategy: " + namingStrategy);
        try
        {
          @SuppressWarnings("unchecked")
          Class<NamingStrategy> namingStrategyClass = (Class<NamingStrategy>) Class.forName(namingStrategy);
          config.setNamingStrategy(namingStrategyClass.newInstance());
        }
        catch (Exception e)
        {
          getLog().error("Error setting NamingStrategy", e);
          throw new MojoExecutionException(e.getMessage());
        }
      }


      Environment.verifyProperties(config.getProperties());
      ConfigurationHelper.resolvePlaceHolders(config.getProperties());
      registry =
          (StandardServiceRegistryImpl)
          new StandardServiceRegistryBuilder()
              .applySettings(config.getProperties())
              .build();

      config.buildMappings();

      if (envers)
      {
        getLog().info("Automatic auditing via hibernate-envers enabled!");
        AuditConfiguration.getFor(config);
      }

      SchemaExport export = new SchemaExport(registry, config);
      export.setDelimiter(delimiter);
      export.setFormat(format);

      File outF = new File(outputFile);

      if (!outF.isAbsolute())
      {
        // Interpret relative file path relative to build directory
        outF = new File(buildDirectory, outputFile);
        getLog().info("Adjusted relative path, resulting path is " + outF.getPath());
      }

      // Ensure that directory path for specified file exists
      File outFileParentDir = outF.getParentFile();
      if (null != outFileParentDir && !outFileParentDir.exists())
      {
        try
        {
          getLog().info("Creating directory path for output file:" + outFileParentDir.getPath());
          outFileParentDir.mkdirs();
        }
        catch (Exception e)
        {
          getLog().error("Error creating directory path for output file: " + e.getLocalizedMessage());
        }
      }

      export.setOutputFile(outF.getPath());
      export.execute(target, type);

      for (Object exception : export.getExceptions())
        getLog().debug(exception.toString());
    }
    finally
    {
      /** Stop Log-Capturing */
      MavenLogAppender.endPluginLog(this);

      /** Restore the old class-loader (TODO: is this really necessary?) */
      Thread.currentThread().setContextClassLoader(contextClassLoader);

      if (registry != null)
        registry.destroy();
    }

    /** Write md5-sums for annotated classes to file */
    try
    {
      FileOutputStream fos = new FileOutputStream(saved);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(md5s);
      oos.close();
      fos.close();
    }
    catch (Exception e)
    {
      getLog().error("Cannot write md5-sums to file: " + e);
    }
  }

  private ParsedPersistenceXmlDescriptor getPersistenceUnitDescriptor(
      String name,
      Properties properties,
      ClassLoaderService loader
      )
      throws
        MojoFailureException
  {
    PersistenceXmlParser parser =
        new PersistenceXmlParser(
            loader,
            PersistenceUnitTransactionType.RESOURCE_LOCAL
             );

    List<ParsedPersistenceXmlDescriptor> units = parser.doResolve(properties);

    if (name == null)
    {
      switch (units.size())
      {
        case 0:
          getLog().info("Found no META-INF/persistence.xml.");
          return null;
        case 1:
          getLog().info("Using persistence-unit " + units.get(0).getName());
          return units.get(0);
        default:
          getLog().warn("No name provided and multiple persistence units found:");
          for (ParsedPersistenceXmlDescriptor unit : units)
            getLog().warn(" - " + unit.getName());
          return null;
      }

    }

    for (ParsedPersistenceXmlDescriptor unit : units)
    {
      getLog().debug("Found persistence-unit " + unit.getName());
      if (!unit.getName().equals(name))
        continue;

      // See if we (Hibernate) are the persistence provider
      if (!ProviderChecker.isProvider(unit, properties))
      {
        getLog().debug("Wrong provider: " + unit.getProviderClassName());
        continue;
      }

      getLog().info("Using persistence-unit " + unit.getName());
      return unit;
    }

    throw new MojoFailureException("Could not find persistence-unit " + name);
  }


  static final class MavenProjectClassLoaderService implements ClassLoaderService
  {
    final private ClassLoader loader;


    public MavenProjectClassLoaderService(ClassLoader loader)
    {
      this.loader = loader;
    }


    @Override
    public <T> Class<T> classForName(String name)
    {
      try
      {
        return (Class<T>)loader.loadClass(name);
      }
      catch (ClassNotFoundException e)
      {
        throw new ClassLoadingException( "Unable to load class [" + name + "]", e );
      }
    }

    @Override
    public URL locateResource(String name)
    {
      return loader.getResource(name);
    }

    @Override
    public InputStream locateResourceStream(String name)
    {
      return loader.getResourceAsStream(name);
    }

    @Override
    public List<URL> locateResources(String name)
    {
      try
      {
        return Collections.list(loader.getResources(name));
      }
      catch (IOException e)
      {
        return Collections.EMPTY_LIST;
      }
    }

    @Override
    public <S> LinkedHashSet<S> loadJavaServices(Class<S> serviceContract)
    {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void stop() { }

  }


  /**
   * Needed, because DriverManager won't pick up drivers, that were not
   * loaded by the system-classloader!
   * See:
   * http://stackoverflow.com/questions/288828/how-to-use-a-jdbc-driver-fromodifiedm-an-arbitrary-location
   */
  static final class DriverProxy implements Driver
  {
    private final Driver target;

    DriverProxy(Driver target)
    {
      if (target == null)
        throw new NullPointerException();
      this.target = target;
    }

    public java.sql.Driver getTarget()
    {
      return target;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException
    {
      return target.acceptsURL(url);
    }

    @Override
    public java.sql.Connection connect(
        String url,
        java.util.Properties info
      )
      throws
        SQLException
    {
      return target.connect(url, info);
    }

    @Override
    public int getMajorVersion()
    {
      return target.getMajorVersion();
    }

    @Override
    public int getMinorVersion()
    {
      return target.getMinorVersion();
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(
        String url,
        Properties info
      )
      throws
        SQLException
    {
      return target.getPropertyInfo(url, info);
    }

    @Override
    public boolean jdbcCompliant()
    {
      return target.jdbcCompliant();
    }

    /**
     * This Method cannot be annotated with @Override, becaus the plugin
     * will not compile then under Java 1.6!
     */
    public Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
      throw new SQLFeatureNotSupportedException("Not supported, for backward-compatibility with Java 1.6");
    }

    @Override
    public String toString()
    {
      return "Proxy: " + target;
    }

    @Override
    public int hashCode()
    {
      return target.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
      if (!(obj instanceof DriverProxy))
        return false;
      DriverProxy other = (DriverProxy) obj;
      return this.target.equals(other.target);
    }
  }
}
