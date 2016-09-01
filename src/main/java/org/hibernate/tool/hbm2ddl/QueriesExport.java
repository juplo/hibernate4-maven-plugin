/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import com.google.common.collect.Lists;
import org.hibernate.HibernateException;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;

import java.io.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Commandline tool to export named queries into a sql file. This class may also be called from inside an application.
 *
 * @author Benoit Lavenier
 */
public class QueriesExport {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( QueriesExport.class );

	private static final String DEFAULT_IMPORT_FILE = "/queries.sql";

	private final SqlStatementLogger sqlStatementLogger;
	private final ClassLoaderService classLoaderService;
	private final String[] queriesSQL;
	private final String importFiles;

	private final List<Exception> exceptions = new ArrayList<Exception>();

	private Formatter formatter;
	private ImportSqlCommandExtractor importSqlCommandExtractor = ImportSqlCommandExtractorInitiator.DEFAULT_EXTRACTOR;

	private String outputFile;
	private String delimiter;

	/**
	 * Builds a QueriesExport object.
	 *
	 * @param metadata The metadata object holding the mapping info to be exported
	 */
	public QueriesExport(MetadataImplementor metadata) {
		this( metadata.getMetadataBuildingOptions().getServiceRegistry(), metadata);
	}

	/**
	 * Builds a QueriesExport object.
	 *
	 * @param serviceRegistry The registry of services available for use.  Should, at a minimum, contain
	 * the JdbcServices service.
	 * @param metadata The metadata object holding the mapping info to be exported
	 */
	public QueriesExport(
			ServiceRegistry serviceRegistry,
			MetadataImplementor metadata) {
		this.sqlStatementLogger = serviceRegistry.getService( JdbcServices.class ).getSqlStatementLogger();
		this.formatter = ( sqlStatementLogger.isFormat() ? FormatStyle.BASIC : FormatStyle.NONE ).getFormatter();
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		this.importFiles = ConfigurationHelper.getString(
				AvailableSettings.HBM2DDL_IMPORT_FILES,
				serviceRegistry.getService( ConfigurationService.class ).getSettings(),
				DEFAULT_IMPORT_FILE
		);

		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor)metadata.buildSessionFactory();
		ASTQueryTranslatorFactory queryTranslatorFactory = new ASTQueryTranslatorFactory();

		List<String> lines = Lists.newArrayList();

		// named queries
		for (NamedQueryDefinition queryDef: metadata.getNamedQueryDefinitions()) {
			String name = queryDef.getName();
			String comment = queryDef.getComment();
			String hql = queryDef.getQueryString();
			Map<String, String> params = queryDef.getParameterTypes();

			// Convert HQL to SQL
			QueryTranslator queryTranslator = queryTranslatorFactory.createQueryTranslator(name,
					hql, java.util.Collections.EMPTY_MAP, sessionFactory, null);
			queryTranslator.compile(java.util.Collections.EMPTY_MAP, false);
			String sql;
			if (queryTranslator.isManipulationStatement()) {
				// Add to the result list
				addQuery(lines, name, comment, params, hql, null);
				for (String line: queryTranslator.collectSqlStrings()) {
					lines.add(line);
				}
			}
			else {
				sql = queryTranslator.getSQLString();
				// Add to the result list
				addQuery(lines, name, comment, params, hql, sql);
			}

		}

		// native queries
		for (NamedSQLQueryDefinition queryDef: metadata.getNamedNativeQueryDefinitions()) {
			String name = queryDef.getName();
			String comment = queryDef.getComment();
			String sql = queryDef.getQueryString();
			Map<String, String> params = queryDef.getParameterTypes();

			// Add to the result list
			addQuery(lines, name, comment, params, null, sql);
		}

		this.queriesSQL = lines.toArray( new String[lines.size()] );
	}

	public QueriesExport(
			String[] queriesSQL) {
		this.queriesSQL = queriesSQL;
		this.importFiles = "";
		this.sqlStatementLogger = new SqlStatementLogger( false, true );
		this.classLoaderService = new ClassLoaderServiceImpl();
		this.formatter = FormatStyle.BASIC.getFormatter();
	}

	/**
	 * For generating a export script file, this is the file which will be written.
	 *
	 * @param filename The name of the file to which to write the export script.
	 *
	 * @return this
	 */
	public QueriesExport setOutputFile(String filename) {
		outputFile = filename;
		return this;
	}

	/**
	 * Set the end of statement delimiter
	 *
	 * @param delimiter The delimiter
	 *
	 * @return this
	 */
	public QueriesExport setDelimiter(String delimiter) {
		this.delimiter = delimiter;
		return this;
	}

	/**
	 * Should we format the sql strings?
	 *
	 * @param format Should we format SQL strings
	 *
	 * @return this
	 */
	public QueriesExport setFormat(boolean format) {
		this.formatter = ( format ? FormatStyle.BASIC : FormatStyle.NONE ).getFormatter();
		return this;
	}

	/**
	 * Set <i>import.sql</i> command extractor. By default {@link org.hibernate.tool.hbm2ddl.SingleLineSqlCommandExtractor} is used.
	 *
	 * @param importSqlCommandExtractor <i>import.sql</i> command extractor.
	 *
	 * @return this
	 */
	public QueriesExport setImportSqlCommandExtractor(ImportSqlCommandExtractor importSqlCommandExtractor) {
		this.importSqlCommandExtractor = importSqlCommandExtractor;
		return this;
	}

	/**
	 * Run the queries extraction
	 *
	 */
	public void execute(boolean script) {
		if (outputFile == null ) {
			return;
		}
		exceptions.clear();

		LOG.info("Starting queries extraction");

		final List<NamedReader> importFileReaders = new ArrayList<NamedReader>();
		for ( String currentFile : importFiles.split( "," ) ) {
			final String resourceName = currentFile.trim();

			InputStream stream = classLoaderService.locateResourceStream( resourceName );
			if ( stream == null ) {
				LOG.debugf( "Import file not found: %s", currentFile );
			}
			else {
				importFileReaders.add( new NamedReader( resourceName, stream ) );
			}
		}

		final List<Exporter> exporters = new ArrayList<Exporter>();
		try {
			// prepare exporters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			if ( script ) {
				exporters.add( new ScriptExporter() );
			}
			if ( outputFile != null ) {
				exporters.add( new FileExporter( outputFile ) );
			}

			// perform exporters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			perform(queriesSQL, exporters );
			if ( !importFileReaders.isEmpty() ) {
				for ( NamedReader namedReader : importFileReaders ) {
					importScript( namedReader, exporters );
				}
			}
		}
		catch (Exception e) {
			exceptions.add( e );
			LOG.error("Could not extract queries", e);
		}
		finally {
			// release exporters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			for ( Exporter exporter : exporters ) {
				try {
					exporter.release();
				}
				catch (Exception ignore) {
				}
			}

			// release the named readers from import scripts
			for ( NamedReader namedReader : importFileReaders ) {
				try {
					namedReader.getReader().close();
				}
				catch (Exception ignore) {
				}
			}
			LOG.info("Successfully extract queries");
		}
	}

	private void perform(String[] lines, List<Exporter> exporters) {
		for ( String line : lines ) {
			String formatted;
			if (line.trim().length() > 0 && !line.startsWith("--")) {
				formatted = formatter.format(line);
				if (delimiter != null) {
					formatted += delimiter;
				}
				sqlStatementLogger.logStatement( line, formatter );
			}
			else {
				formatted = line;
			}
			for ( Exporter exporter : exporters ) {
				try {
					exporter.export( formatted );
				}
				catch (Exception e) {
					exceptions.add( e );
					LOG.debug("Unable to perform command: " + line );
					LOG.error( e.getMessage() );
				}
			}
		}
	}

	private void importScript(NamedReader namedReader, List<Exporter> exporters) throws Exception {
		BufferedReader reader = new BufferedReader( namedReader.getReader() );
		String[] statements = importSqlCommandExtractor.extractCommands( reader );
		if ( statements != null ) {
			for ( String statement : statements ) {
				if ( statement != null ) {
					String trimmedSql = statement.trim();
					if ( trimmedSql.endsWith( ";" ) ) {
						trimmedSql = trimmedSql.substring( 0, statement.length() - 1 );
					}
					if ( !StringHelper.isEmpty( trimmedSql ) ) {
						try {
							for ( Exporter exporter : exporters ) {
								if ( exporter.acceptsImportScripts() ) {
									exporter.export( trimmedSql );
								}
							}
						}
						catch (Exception e) {
							exceptions.add( e );
							LOG.unsuccessful( trimmedSql );
							LOG.error( e.getMessage() );
						}
					}
				}
			}
		}
	}

	private void addQuery(List<String> lines, String name, String comment, Map<String, String> params, String hql, String sql) {

		// header
		if (lines.size() == 0) {
			lines.add("--");
			lines.add("-- Queries extraction file");
			lines.add("--");
		}
		else {
			lines.add("");
			lines.add("");
		}
		lines.add("-- ************************************************************");

		// Comment
		if (comment != null && comment.trim().length() > 0) {
			String[] commentLines = comment.trim().split("\n");
			for (String line: commentLines) {
				lines.add("-- " + line.trim());
			}
			lines.add("--");
		}

		// name
		lines.add("-- @name\t\t\t" + name);

		// parameters
		if (params != null && !params.isEmpty()) {
			for (String paramName: params.keySet()) {
				String paramType = params.get(paramName);
				lines.add(String.format("-- @param\t\t\t%s\t\t%s", paramName, paramType));
			}
		}

		// hql
		if (hql != null && hql.trim().length() > 0) {
			lines.add("--");
			lines.add("-- @hql");
			String[] hqlLines = hql.trim().split("\n");
			for (String line : hqlLines) {
				if (line.trim().length() > 0) {
					lines.add("--   " + line);
				}
			}
		}
		lines.add("--");


		lines.add("-- ************************************************************");

		// sql (could be null)
		if (sql != null && sql.trim().length() > 0) {
			lines.add(sql.trim());
		}
	}

	private static class NamedReader {
		private final Reader reader;
		private final String name;

		public NamedReader(String name, InputStream stream) {
			this.name = name;
			this.reader = new InputStreamReader( stream );
		}

		public Reader getReader() {
			return reader;
		}

		public String getName() {
			return name;
		}
	}

	public static void main(String[] args) {
		try {
			final CommandLineArgs commandLineArgs = CommandLineArgs.parseCommandLineArgs( args );
			StandardServiceRegistry serviceRegistry = buildStandardServiceRegistry( commandLineArgs );
			try {
				final MetadataImplementor metadata = buildMetadata( commandLineArgs, serviceRegistry );

				QueriesExport queriesExport = new QueriesExport( serviceRegistry, metadata)
						.setOutputFile( commandLineArgs.outputFile )
						.setDelimiter( commandLineArgs.delimiter )
						.setImportSqlCommandExtractor( serviceRegistry.getService( ImportSqlCommandExtractor.class ) )
						.setFormat( commandLineArgs.format );
				queriesExport.execute(commandLineArgs.script);
			}
			finally {
				StandardServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
		catch (Exception e) {
			LOG.unableToCreateSchema( e );
			e.printStackTrace();
		}
	}

	private static StandardServiceRegistry buildStandardServiceRegistry(CommandLineArgs commandLineArgs)
			throws Exception {
		final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
		final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder( bsr );

		if ( commandLineArgs.cfgXmlFile != null ) {
			ssrBuilder.configure( commandLineArgs.cfgXmlFile );
		}

		Properties properties = new Properties();
		if ( commandLineArgs.propertiesFile != null ) {
			properties.load( new FileInputStream( commandLineArgs.propertiesFile ) );
		}
		ssrBuilder.applySettings( properties );

		if ( commandLineArgs.importFile != null ) {
			ssrBuilder.applySetting( AvailableSettings.HBM2DDL_IMPORT_FILES, commandLineArgs.importFile );
		}

		return ssrBuilder.build();
	}

	private static MetadataImplementor buildMetadata(
			CommandLineArgs parsedArgs,
			StandardServiceRegistry serviceRegistry) throws Exception {
		final MetadataSources metadataSources = new MetadataSources( serviceRegistry );

		for ( String filename : parsedArgs.hbmXmlFiles ) {
			metadataSources.addFile( filename );
		}

		for ( String filename : parsedArgs.jarFiles ) {
			metadataSources.addJar( new File( filename ) );
		}


		final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();

		return (MetadataImplementor) metadataBuilder.build();
	}

	/**
	 * Returns a List of all Exceptions which occured during the export.
	 *
	 * @return A List containig the Exceptions occured during the export
	 */
	public List getExceptions() {
		return exceptions;
	}

	private static class CommandLineArgs {
		boolean script = true;
		boolean format = false;

		String delimiter = null;

		String outputFile = null;
		String importFile = DEFAULT_IMPORT_FILE;

		String propertiesFile = null;
		String cfgXmlFile = null;

		List<String> hbmXmlFiles = new ArrayList<String>();
		List<String> jarFiles = new ArrayList<String>();

		public static CommandLineArgs parseCommandLineArgs(String[] args) {
			CommandLineArgs parsedArgs = new CommandLineArgs();

			for ( String arg : args ) {
				if ( arg.startsWith( "--" ) ) {
					if ( arg.equals( "--quiet" ) ) {
						parsedArgs.script = false;
					}
					else if ( arg.startsWith( "--output=" ) ) {
						parsedArgs.outputFile = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--import=" ) ) {
						parsedArgs.importFile = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--properties=" ) ) {
						parsedArgs.propertiesFile = arg.substring( 13 );
					}
					else if ( arg.equals( "--format" ) ) {
						parsedArgs.format = true;
					}
					else if ( arg.startsWith( "--delimiter=" ) ) {
						parsedArgs.delimiter = arg.substring( 12 );
					}
					else if ( arg.startsWith( "--config=" ) ) {
						parsedArgs.cfgXmlFile = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--naming=" ) ) {
						DeprecationLogger.DEPRECATION_LOGGER.logDeprecatedNamingStrategyArgument();
					}
				}
				else {
					if ( arg.endsWith( ".jar" ) ) {
						parsedArgs.jarFiles.add( arg );
					}
					else {
						parsedArgs.hbmXmlFiles.add( arg );
					}
				}
			}

			return parsedArgs;
		}
	}
}
