package de.juplo.plugins.hibernate;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.plugin.logging.Log;



/**
 *
 * @author Kai Moritz
 */
public class ModificationTracker
{
  private Map<String,String> properties;
  private Map<String,String> classes;

  private final Set<String> propertyNames;
  private final Set<String> classNames;

  private boolean modified = false;

  private final File saved;
  private final MessageDigest digest;
  private final Log log;


  ModificationTracker(String buildDirectory, String filename, Log log)
      throws
        NoSuchAlgorithmException
  {
    propertyNames = new HashSet<String>();
    classNames = new HashSet<String>();
    File output = new File(filename + ".md5s");
    if (output.isAbsolute())
    {
      saved = output;
    }
    else
    {
      // Interpret relative file path relative to build directory
      saved = new File(buildDirectory, output.getPath());
      log.debug("Adjusted relative path, resulting path is " + saved.getPath());
    }
    digest = java.security.MessageDigest.getInstance("MD5");
    this.log = log;
  }


  private String calculate(InputStream is)
      throws
        IOException
  {
    byte[] buffer = new byte[1024*4]; // copy data in 4MB-chunks
    int i;
    while((i = is.read(buffer)) > -1)
      digest.update(buffer, 0, i);
    is.close();
    byte[] bytes = digest.digest();
    BigInteger bi = new BigInteger(1, bytes);
    return String.format("%0" + (bytes.length << 1) + "x", bi);
  }

  private boolean check(Map<String,String> values, String name, String value)
  {
    if (!values.containsKey(name) || !values.get(name).equals(value))
    {
      values.put(name, value);
      return true;
    }
    else
      return false;
  }


  boolean track(String name, InputStream is) throws IOException
  {
    boolean result = check(classes, name, calculate(is));
    classNames.add(name);
    modified |= result;
    return result;
  }


  boolean check(String name, String property)
  {
    propertyNames.add(name);
    return check(properties, name, property);
  }

  boolean track(String name, String property)
  {
    boolean result = check(name, property);
    modified |= result;
    return result;
  }

  boolean track(Properties properties)
  {
    boolean result = false;
    for (String name : properties.stringPropertyNames())
      result |= track(name, properties.getProperty(name));
    return result;
  }


  void touch()
  {
    modified = true;
  }

  boolean modified()
  {
    modified |= !propertyNames.containsAll(properties.keySet());
    modified |= !properties.keySet().containsAll(propertyNames);
    modified |= !classNames.containsAll(classes.keySet());
    modified |= !classes.keySet().containsAll(classNames);
    return modified;
  }


  void load()
  {
    if (saved.isFile() && saved.length() > 0)
    {
      try
      {
        FileInputStream fis = new FileInputStream(saved);
        ObjectInputStream ois = new ObjectInputStream(fis);
        properties = (HashMap<String,String>)ois.readObject();
        classes = (HashMap<String,String>)ois.readObject();
        ois.close();
      }
      catch (Exception e)
      {
        properties = new HashMap<String,String>();
        classes = new HashMap<String,String>();
        log.warn("Cannot read md5s from saved: " + e);
      }
    }
    else
    {
      properties = new HashMap<String,String>();
      classes = new HashMap<String,String>();
      try
      {
        saved.createNewFile();
      }
      catch (IOException e)
      {
        log.debug("Cannot create file \"" + saved.getPath() + "\" for md5s: " + e);
      }
    }
  }

  void save()
  {
    if (!modified)
      return;

    /** Write md5-sums for annotated classes to file */
    try
    {
      FileOutputStream fos = new FileOutputStream(saved);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(properties);
      oos.writeObject(classes);
      oos.close();
      fos.close();
    }
    catch (Exception e)
    {
      log.error("Cannot write md5-sums to file: " + e);
    }
  }  
}
