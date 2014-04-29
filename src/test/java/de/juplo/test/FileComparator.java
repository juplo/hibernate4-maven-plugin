package de.juplo.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class FileComparator
{
  private final File basedir;
  private BufferedReader expectedReader;
  private BufferedReader foundReader;

  public FileComparator(File basedir)
  {
    this.basedir = basedir;
  }

  public boolean isEqual(final String expectedFile, final String foundFile)
    throws
      FileNotFoundException,
      IOException
  {
    File file;
    String expected, found;

    file = new File(basedir, expectedFile);
    expectedReader = new BufferedReader(new FileReader(file));

    file = new File(basedir, foundFile);
    foundReader = new BufferedReader(new FileReader(file));


    while ((expected = expectedReader.readLine()) != null)
    {
      found = foundReader.readLine();
      if (!expected.equals(found))
      {
        System.err.println("Mismatch!");
        System.err.println("Expected: " + expected);
        System.err.println("Found:    " + found);
        return false;
      }
    }

    if ((found = foundReader.readLine()) != null)
    {
      System.err.println("Found more content than expected!");
      System.err.println("Starting with: " + found);
      return false;
    }

    return true;
  }
}