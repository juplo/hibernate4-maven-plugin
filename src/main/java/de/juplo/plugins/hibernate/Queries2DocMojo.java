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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.QueriesExport;
import org.hibernate.tool.hbm2doc.queries.Queries2DocExport;

import java.io.File;


/**
 * Goal which extracts the hibernate-mapping-configuration and
 * exports all queries to HTML documentation.
 *
 * @goal queries2doc
 * @phase process-classes
 * @threadSafe
 * @requiresDependencyResolution runtime
 */
public class Queries2DocMojo extends AbstractSchemaMojo
{
  /**
   * Output file.
   * <p>
   * Relative to
   * (<code>project.build.directory</code>).
   *
   * @parameter property="hibernate.hbm2doc.queries.directory" default-value="site/queries/index.flt"
   * @since 1.0
   */
  private String outputFile;

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
   * @parameter property="hibernate.queries2doc.skip" default-value="${maven.site.skip}"
   * @since 1.0
   */
  private boolean skip;

  @Override
  public final void execute()
    throws
      MojoFailureException,
      MojoExecutionException
  {
    setSkip(skip);
    super.execute(outputFile);
  }

  @Override
  void build(MetadataImplementor metadata)
      throws
        MojoExecutionException,
        MojoFailureException
  {

    Queries2DocExport queriesExport = new Queries2DocExport(metadata);
    queriesExport.setDelimiter(delimiter);

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

    queriesExport.setOutputDirectory(output.getParentFile().getPath());
    queriesExport.execute();
  }
}
