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
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.hibernate.cfg.Configuration;
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
  public final static String DRIVER_CLASS = "hibernate.connection.driver_class";
  public final static String URL = "hibernate.connection.url";
  public final static String USERNAME = "hibernate.connection.username";
  public final static String PASSWORD = "hibernate.connection.password";
  public final static String DIALECT = "hibernate.dialect";


  /**
   * The project whose project files to create.
   *
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * Directories to scan.
   *
   * @parameter expression="${project.build.outputDirectory}"
   */
  private String outputDirectory;

  /**
   * Skip execution
   *
   * @parameter expression="${maven.test.skip}"
   */
  private boolean skip;

  /**
   * SQL-Driver name.
   *
   * @parameter expression="${hibernate.connection.driver_class}
   */
  private String driverClassName;

  /**
   * Database URL.
   *
   * @parameter expression="${hibernate.connection.url}"
   */
  private String url;

  /**
   * Database username
   *
   * @parameter expression="${hibernate.connection.username}"
   */
  private String username;

  /**
   * Database password
   *
   * @parameter expression="${hibernate.connection.password}"
   */
  private String password;

  /**
   * Hibernate dialect.
   *
   * @parameter expression="${hibernate.dialect}"
   */
  private String hibernateDialect;

  /**
   * Hibernate configuration file.
   *
   * @parameter default-value="${project.build.outputDirectory}/hibernate.properties"
   */
  private String hibernateProperties;

  /**
   * Target of execution:
   * <ul>
   *   <li><strong>NONE</strong> do nothing - just validate the configuration</li>
   *   <li><strong>EXPORT</strong> create database <strong>(DEFAULT!)</strong></li>
   *   <li><strong>SCRIPT</strong> export schema to SQL-script</li>
   *   <li><strong>BOTH</strong></li>
   * </ul>
   * @parameter default-value="EXPORT"
   */
  private String target;

  /**
   * Type of export.
   * <ul>
   *   <li><strong>NONE</strong> do nothing - just validate the configuration</li>
   *   <li><strong>CREATE</strong> create database-schema</li>
   *   <li><strong>DROP</strong> drop database-schema</li>
   *   <li><strong>BOTH</strong> <strong>(DEFAULT!)</strong></li>
   * </ul>
   * @parameter default-value="BOTH"
   */
  private String type;

  /**
   * Output file.
   *
   * @parameter default-value="${project.build.outputDirectory}/schema.sql"
   */
  private String outputFile;

  /**
   * Delimiter in output-file.
   *
   * @parameter default-value=";"
   */
  private String delimiter;

  /**
   * Format output-file.
   *
   * @parameter default-value="true"
   */
  private boolean format;


  @Override
  public void execute()
    throws
      MojoFailureException,
      MojoExecutionException
  {
    if (skip)
      return;

    File dir = new File(outputDirectory);
    if (!dir.exists())
      throw new MojoExecutionException("Cannot scan for annotated classes in " + outputDirectory + ": directory does not exist!");


    Set<String> classes = new HashSet<String>();
    try
    {
      AnnotationDB db = new AnnotationDB();
      getLog().info("Scanning directory " + outputDirectory + " for annotated classes...");
      URL dirUrl = dir.toURI().toURL();
      db.scanArchives(dirUrl);
      if (db.getAnnotationIndex().containsKey(Entity.class.getName()))
        classes.addAll(db.getAnnotationIndex().get(Entity.class.getName()));
      if (db.getAnnotationIndex().containsKey(MappedSuperclass.class.getName()))
        classes.addAll(db.getAnnotationIndex().get(MappedSuperclass.class.getName()));
      if (db.getAnnotationIndex().containsKey(Embeddable.class.getName()))
        classes.addAll(db.getAnnotationIndex().get(Embeddable.class.getName()));
    }
    catch (IOException e)
    {
      getLog().error("Error while scanning!", e);
      throw new MojoFailureException(e.getMessage());
    }
    if (classes.isEmpty())
      throw new MojoFailureException("No annotated classes found in directory " + outputDirectory);

    Properties properties = new Properties();

    /** Try to read configuration from properties-file */
    try
    {
      File file = new File(hibernateProperties);
      if (file.exists())
      {
        getLog().info("Reading properties from file " + hibernateProperties + "...");
        properties.load(new FileInputStream(file));
      }
      else
        getLog().info("No hibernate-properties-file found! Checked path: " + hibernateProperties);
    }
    catch (IOException e)
    {
      getLog().error("Error while reading properties!", e);
      throw new MojoExecutionException(e.getMessage());
    }

    /** Overwrite values from propertie-file or set, if given */
    if (driverClassName != null)
    {
      if (properties.containsKey(DRIVER_CLASS))
        getLog().debug(
            "Overwriting property " +
            DRIVER_CLASS + "=" + properties.getProperty(DRIVER_CLASS) +
            " with the value " + driverClassName +
            " from the plugin-configuration-parameter driverClassName!"
          );
      else
        getLog().debug(
            "Using the value " + driverClassName +
            " from the plugin-configuration-parameter driverClassName!"
          );
      properties.setProperty(DRIVER_CLASS, driverClassName);
    }
    if (url != null)
    {
      if (properties.containsKey(URL))
        getLog().debug(
            "Overwriting property " +
            URL + "=" + properties.getProperty(URL) +
            " with the value " + url +
            " from the plugin-configuration-parameter url!"
          );
      else
        getLog().debug(
            "Using the value " + url +
            " from the plugin-configuration-parameter url!"
          );
      properties.setProperty(URL, url);
    }
    if (username != null)
    {
      if (properties.containsKey(USERNAME))
        getLog().debug(
            "Overwriting property " +
            USERNAME + "=" + properties.getProperty(USERNAME) +
            " with the value " + username +
            " from the plugin-configuration-parameter username!"
          );
      else
        getLog().debug(
            "Using the value " + username +
            " from the plugin-configuration-parameter username!"
          );
      properties.setProperty(USERNAME, username);
    }
    if (password != null)
    {
      if (properties.containsKey(PASSWORD))
        getLog().debug(
            "Overwriting property " +
            PASSWORD + "=" + properties.getProperty(PASSWORD) +
            " with the value " + password +
            " from the plugin-configuration-parameter password!"
          );
      else
        getLog().debug(
            "Using the value " + password +
            " from the plugin-configuration-parameter password!"
          );
      properties.setProperty(PASSWORD, password);
    }
    if (hibernateDialect != null)
    {
      if (properties.containsKey(DIALECT))
        getLog().debug(
            "Overwriting property " +
            DIALECT + "=" + properties.getProperty(DIALECT) +
            " with the value " + hibernateDialect +
            " from the plugin-configuration-parameter hibernateDialect!"
          );
      else
        getLog().debug(
            "Using the value " + hibernateDialect +
            " from the plugin-configuration-parameter hibernateDialect!"
          );
      properties.setProperty(DIALECT, hibernateDialect);
    }

    getLog().info("Gathered hibernate-configuration (turn on debugging for details):");
    if (properties.isEmpty())
    {
      getLog().error("No properties set!");
      throw new MojoFailureException("Hibernate-Configuration is missing!");
    }
    for (Entry<Object,Object> entry : properties.entrySet())
      getLog().info("  " + entry.getKey() + " = " + entry.getValue());

    ClassLoader classLoader = null;
    try
    {
      getLog().debug("Creating ClassLoader for project-dependencies...");
      List<String> classpathFiles = project.getCompileClasspathElements();
      URL[] urls = new URL[classpathFiles.size()];
      for (int i = 0; i < classpathFiles.size(); ++i)
      {
        getLog().debug("Dependency: " + classpathFiles.get(i));
        urls[i] = new File(classpathFiles.get(i)).toURI().toURL();
      }
      classLoader = new URLClassLoader(urls, getClass().getClassLoader());
    }
    catch (Exception e)
    {
      getLog().error("Error while creating ClassLoader!", e);
      throw new MojoExecutionException(e.getMessage());
    }

    Configuration config = new Configuration();
    config.setProperties(properties);
    try
    {
      getLog().debug("Adding annotated classes to hibernate-mapping-configuration...");
      for (String annotatedClass : classes)
      {
        getLog().debug("Class " + annotatedClass);
        config.addAnnotatedClass(classLoader.loadClass(annotatedClass));
      }
    }
    catch (ClassNotFoundException e)
    {
      getLog().error("Error while adding annotated classes!", e);
      throw new MojoExecutionException(e.getMessage());
    }

    Target target = null;
    try
    {
      target = Target.valueOf(this.target);
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
      type = Type.valueOf(this.type);
    }
    catch (IllegalArgumentException e)
    {
      getLog().error("Invalid value for configuration-option \"type\": " + this.type);
      getLog().error("Valid values are: NONE, CREATE, DROP, BOTH");
      throw new MojoExecutionException("Invalid value for configuration-option \"type\"");
    }

    Connection connection = null;
    try
    {
      /**
       * The connection must be established outside of hibernate, because
       * hibernate does not use the context-classloader of the current
       * thread and, hence, would not be able to resolve the driver-class!
       */
      switch (target)
      {
        case EXPORT:
        case BOTH:
          switch (type)
          {
            case CREATE:
            case DROP:
            case BOTH:
              Class driverClass = classLoader.loadClass(driverClassName);
              getLog().debug("Registering JDBC-driver " + driverClass.getName());
              DriverManager.registerDriver(new DriverProxy((Driver)driverClass.newInstance()));
              getLog().debug("Opening JDBC-connection to " + url + " as " + username + " with password " + password);
              connection = DriverManager.getConnection(url, username, password);
          }
      }
    }
    catch (ClassNotFoundException e)
    {
      getLog().error("Dependency for driver-class " + driverClassName + " is missing!");
      throw new MojoExecutionException(e.getMessage());
    }
    catch (Exception e)
    {
      getLog().error("Cannot establish connection to database!");
      Enumeration<Driver> drivers = DriverManager.getDrivers();
      if (!drivers.hasMoreElements())
        getLog().error("No drivers registered!");
      while (drivers.hasMoreElements())
        getLog().debug("Driver: " + drivers.nextElement());
      throw new MojoExecutionException(e.getMessage());
    }

    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    MavenLogAppender.startPluginLog(this);
    try
    {
      /**
       * Change class-loader of current thread, so that hibernate can
       * see all dependencies!
       */
      Thread.currentThread().setContextClassLoader(classLoader);

      SchemaExport export = new SchemaExport(config, connection);
      export.setOutputFile(outputFile);
      export.setDelimiter(delimiter);
      export.setFormat(format);
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

      /** Close the connection */
      try
      {
        connection.close();
      }
      catch (SQLException e)
      {
        getLog().error("Error while closing connection: " + e.getMessage());
      }
    }
  }

  /**
   * Needed, because DriverManager won't pick up drivers, that were not
   * loaded by the system-classloader!
   * See:
   * http://stackoverflow.com/questions/288828/how-to-use-a-jdbc-driver-from-an-arbitrary-location
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
