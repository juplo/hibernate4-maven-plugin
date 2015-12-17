package de.juplo.plugins.hibernate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import org.apache.maven.plugin.logging.Log;


/**
 *
 * @author kai
 */
public class MutableClassLoader extends ClassLoader
{
  private URLClassLoader loader;
  private Log log;


  public MutableClassLoader(LinkedHashSet<URL> urls, Log log)
  {
    if (log.isDebugEnabled())
      for (URL url : urls)
        log.debug(url.toString());
    loader =
        new URLClassLoader(
            urls.toArray(new URL[urls.size()]),
            getClass().getClassLoader()
            );
    this.log = log;
  }


  public MutableClassLoader add(LinkedHashSet<URL> urls)
  {
    LinkedHashSet<URL> old =
        new LinkedHashSet<URL>(Arrays.asList(loader.getURLs()));
    old.addAll(urls);
    if (log.isDebugEnabled())
      for (URL url : urls)
        log.debug(url.toString());
    loader =
        new URLClassLoader(
            old.toArray(new URL[urls.size()]),
            getClass().getClassLoader()
            );
    return this;
  }


  @Override
  public void clearAssertionStatus()
  {
    loader.clearAssertionStatus();
  }

  @Override
  public void setClassAssertionStatus(String className, boolean enabled)
  {
    loader.setClassAssertionStatus(className, enabled);
  }

  @Override
  public void setPackageAssertionStatus(String packageName, boolean enabled)
  {
    loader.setPackageAssertionStatus(packageName, enabled);
  }

  @Override
  public void setDefaultAssertionStatus(boolean enabled)
  {
    loader.setDefaultAssertionStatus(enabled);
  }

  @Override
  public InputStream getResourceAsStream(String name)
  {
    return loader.getResourceAsStream(name);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException
  {
    return loader.getResources(name);
  }

  @Override
  public URL getResource(String name)
  {
    return loader.getResource(name);
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException
  {
    return loader.loadClass(name);
  }
  
}
