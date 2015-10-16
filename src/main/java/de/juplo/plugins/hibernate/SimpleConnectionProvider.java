package de.juplo.plugins.hibernate;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import static org.eclipse.aether.repository.AuthenticationContext.PASSWORD;
import static org.eclipse.aether.repository.AuthenticationContext.USERNAME;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.PASS;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.AvailableSettings.USER;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import static org.hibernate.jpa.AvailableSettings.JDBC_DRIVER;
import static org.hibernate.jpa.AvailableSettings.JDBC_PASSWORD;
import static org.hibernate.jpa.AvailableSettings.JDBC_URL;
import static org.hibernate.jpa.AvailableSettings.JDBC_USER;

/**
 *
 * @author Kai Moritz
 */
class SimpleConnectionProvider implements ConnectionProvider
{
  private final Log log;

  private Connection connection;


  SimpleConnectionProvider(Log log)
  {
    this.log = log;
  }


  void open(ClassLoaderService classLoaderService, Properties properties)
      throws
        MojoFailureException
  {

    String driver = (String)
        (properties.containsKey(DRIVER)
            ? properties.getProperty(DRIVER)
            : properties.getProperty(JDBC_DRIVER)
            );
    String url = (String)
        (properties.containsKey(URL)
            ? properties.getProperty(URL)
            : properties.getProperty(JDBC_URL)
            );
    String user = (String)
        (properties.containsKey(USER)
            ? properties.getProperty(USER)
            : properties.getProperty(JDBC_USER)
            );
    String password = (String)
        (properties.containsKey(PASS)
            ? properties.getProperty(PASS)
            : properties.getProperty(JDBC_PASSWORD)
            );

    if (driver == null || url == null || user == null)
    {
      log.info("No connection opened, because connection information is incomplete");
      log.info("Driver-Class: " + driver);
      log.info("URL: " + url);
      log.info("User: " + user);
      return;
    }

    try
    {
      Class driverClass = classLoaderService.classForName(driver);

      log.debug("Registering JDBC-driver " + driverClass.getName());
      DriverManager
          .registerDriver(new DriverProxy((Driver) driverClass.newInstance()));

      log.debug(
          "Opening JDBC-connection to " + properties.getProperty(URL) +
          " as " + properties.getProperty(USERNAME) +
          " with password " + properties.getProperty(PASSWORD)
          );
    
      connection = DriverManager.getConnection(url, user, password);
    }
    catch (Exception e)
    {
      throw new MojoFailureException("Could not open the JDBC-connection", e);
    }
  }

  void close()
  {
    if (connection == null)
      return;

    log.debug("Closing the JDBC-connection.");
    try
    {
      connection.close();
    }
    catch (SQLException e)
    {
      log.error("Error while closing the JDBC-connection: " + e.getMessage());
    }
  }

  @Override
  public Connection getConnection() throws SQLException
  {
    log.debug("Connection aquired.");

    if (connection == null)
      throw new SQLException("No connection available, because of insufficient connection information!");

    return connection;
  }

  @Override
  public void closeConnection(Connection conn) throws SQLException
  {
    log.debug("Connection released");
  }

  @Override
  public boolean supportsAggressiveRelease()
  {
    return false;
  }

  @Override
  public boolean isUnwrappableAs(Class unwrapType)
  {
    return false;
  }

  @Override
  public <T> T unwrap(Class<T> unwrapType)
  {
    throw new UnsupportedOperationException("Not supported.");
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
