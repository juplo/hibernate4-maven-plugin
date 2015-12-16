package de.juplo.plugins.hibernate;

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

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;


/**
 * Goal which extracts the hibernate-mapping-configuration and
 * exports an according SQL-database-schema.
 *
 * @goal create
 * @phase process-classes
 * @threadSafe
 * @requiresDependencyResolution runtime
 */
public class CreateMojo extends AbstractSchemaMojo
{
  /**
   * Output file.
   * <p>
   * If the specified filename is not absolut, the file will be created
   * relative to the project build directory
   * (<code>project.build.directory</code>).
   *
   * @parameter property="hibernate.schema.export.create" default-value="schema.sql"
   * @since 1.0
   */
  private String outputFile;


  @Override
  public final void execute()
    throws
      MojoFailureException,
      MojoExecutionException
  {
    super.execute(outputFile);
  }


  @Override
  void build(MetadataImplementor metadata)
      throws
        MojoExecutionException,
        MojoFailureException
  {
    SchemaExport schemaExport = new SchemaExport(metadata, createNamespaces);
    schemaExport.setDelimiter(delimiter);
    schemaExport.setFormat(format);

    File output = new File(outputFile);

    if (!output.isAbsolute())
    {
      // Interpret relative file path relative to build directory
      output = new File(buildDirectory, outputFile);
      getLog().debug("Adjusted relative path, resulting path is " + output.getPath());
    }

    // Ensure that directory path for specified file exists
    File outFileParentDir = output.getParentFile();
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

    schemaExport.setOutputFile(output.getPath());
    schemaExport.execute(false, this.export, false, true);

    for (Object exception : schemaExport.getExceptions())
      getLog().error(exception.toString());
  }
}
