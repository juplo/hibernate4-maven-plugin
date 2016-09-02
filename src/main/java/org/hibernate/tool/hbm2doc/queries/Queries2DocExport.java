/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2doc.queries;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.*;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.jdbc.StreamUtils;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Commandline tool to export named queries into a sql file. This class may also be called from inside an application.
 *
 * @author Benoit Lavenier
 */
public class Queries2DocExport {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( Queries2DocExport.class );

	private final ClassLoaderService classLoaderService;

	private Formatter formatter;
	private ImportSqlCommandExtractor importSqlCommandExtractor = ImportSqlCommandExtractorInitiator.DEFAULT_EXTRACTOR;

	private String outputDirectory;
	private String delimiter;

	private List<DocQuery> queries = new ArrayList<DocQuery>();

	/**
	 * Builds a QueriesExport object.
	 *
	 * @param metadata The metadata object holding the mapping info to be exported
	 */
	public Queries2DocExport(MetadataImplementor metadata) {
		this( metadata.getMetadataBuildingOptions().getServiceRegistry(), metadata);
	}

	/**
	 * Builds a QueriesExport object.
	 *
	 * @param serviceRegistry The registry of services available for use.  Should, at a minimum, contain
	 * the JdbcServices service.
	 * @param metadata The metadata object holding the mapping info to be exported
	 */
	public Queries2DocExport(
			ServiceRegistry serviceRegistry,
			MetadataImplementor metadata) {
		this.formatter = FormatStyle.BASIC.getFormatter();
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor)metadata.buildSessionFactory();
		ASTQueryTranslatorFactory queryTranslatorFactory = new ASTQueryTranslatorFactory();

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

			DocQuery docQuery;
			if (queryTranslator.isManipulationStatement()) {
				// Add to the result list
				List<String> sql = new ArrayList<String>();
				for (String line: queryTranslator.collectSqlStrings()) {
					String formatted = formatter.format(line);
					if (delimiter != null) {
						formatted += delimiter;
					}
					sql.add(formatted.trim());
				}
				docQuery = new DocQuery(name, comment, params, hql.trim(), sql);
			}
			else {
				String sql = queryTranslator.getSQLString();
				if (sql != null) {
					sql = formatter.format(sql);
					if (delimiter != null) {
						sql += delimiter;
					}
				}
				docQuery = new DocQuery(name, comment, params, hql.trim(), sql != null ? sql.trim() : null);
			}
			queries.add(docQuery);
		}

		// native queries
		for (NamedSQLQueryDefinition queryDef: metadata.getNamedNativeQueryDefinitions()) {
			String name = queryDef.getName();
			String comment = queryDef.getComment();
			String sql = queryDef.getQueryString();
			Map<String, String> params = queryDef.getParameterTypes();

			// Add to the result list
			DocQuery docQuery = new DocQuery(name, comment, params, null, sql != null ? sql.trim() : null);
			queries.add(docQuery);
		}
	}

	/**
	 * For generating a doc on queries, this is the output directory.
	 *
	 * @param dirname The path of output directory, to write the generated doc.
	 *
	 * @return this
	 */
	public Queries2DocExport setOutputDirectory(String dirname) {
		outputDirectory= dirname;
		return this;
	}

	/**
	 * Set the end of statement delimiter
	 *
	 * @param delimiter The delimiter
	 *
	 * @return this
	 */
	public Queries2DocExport setDelimiter(String delimiter) {
		this.delimiter = delimiter;
		return this;
	}

	/**
	 * Set <i>import.sql</i> command extractor. By default {@link SingleLineSqlCommandExtractor} is used.
	 *
	 * @param importSqlCommandExtractor <i>import.sql</i> command extractor.
	 *
	 * @return this
	 */
	public Queries2DocExport setImportSqlCommandExtractor(ImportSqlCommandExtractor importSqlCommandExtractor) {
		this.importSqlCommandExtractor = importSqlCommandExtractor;
		return this;
	}

	/**
	 * Run the queries extraction
	 *
	 */
	public void execute() {
		if (outputDirectory == null ) {
			return;
		}

		LOG.info("Starting queries doc generation");

		try {
			perform();
		}
		catch (Exception e) {
			e.printStackTrace();
			LOG.error("Could not generate queries doc", e);
		}
		finally {
			LOG.info("Successfully generate queries doc");
		}
	}

	private void perform() throws Exception {

		Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);

		String packagePath = "org/hibernate/tool/hbm2doc/queries";
		//cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/org/hibernate/tool/hbm2doc/queries");
		cfg.setDirectoryForTemplateLoading(new File("/home/blavenie/git/blavenie/hibernate4-maven-plugin/src/main/resources/org/hibernate/tool/hbm2doc/queries"));

		cfg.setDefaultEncoding("UTF-8");
		// During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
		//TODO : cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);

		cfg.setLogTemplateExceptions(true);

		SimpleHash root = new SimpleHash(cfg.getObjectWrapper());
		root.put("queries", queries);

		// Generate index.flt
		generateFile(cfg, root, "index.flt", "index.html");

		// Generate summary.html
		generateFile(cfg, root, "queries-summary.flt", "queries-summary.html");

		// Generate allqueries.flt
		generateFile(cfg, root, "allqueries.flt", "allqueries.html");

		// TODO gen file for each query
		//

		copyFile(packagePath, "assets/doc-style.css");
	}

	private void generateFile(Configuration cfg, SimpleHash root, String template, String outputFilename)
			throws IOException, TemplateException {

		LOG.debug("Generating " + outputFilename);

		Template temp = cfg.getTemplate(template);
		File outputFile = new File(outputDirectory, outputFilename);

		Writer out = null;
		try {
			out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(outputFile)));
			temp.process(root, out);
		}
		finally {
			if (out != null) {
				try {
					out.close();
				}
				catch(Exception e) {
					// Silent
				}
			}
		}

	}

	private void copyFile(String packagePath, String ressourceFilePath)
			throws IOException, TemplateException {
		copyFile(packagePath, ressourceFilePath, ressourceFilePath);
	}

	private void copyFile(String packagePath, String ressourceFilePath, String outputFilename)
			throws IOException, TemplateException {

		LOG.debug("Copying file " + outputFilename);
		File outputFile = new File(outputDirectory, outputFilename);
		if (!outputFile.getParentFile().exists()) {
			outputFile.getParentFile().mkdirs();
		}
		OutputStream out = null;
		InputStream is = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(outputFile));
			is = this.getClass().getClassLoader().getResourceAsStream(packagePath + "/" + ressourceFilePath);
			StreamUtils.copy(is, out, 2048);
		}
		finally {
			if (out != null) {
				try {
					out.close();
				}
				catch(Exception e) {
					// Silent
				}
			}
			if (is != null) {
				try {
					is.close();
				}
				catch(Exception e) {
					// Silent
				}
			}
		}

	}

	public static class DocQuery {
		private final String name;
		private final String comment;
		private final Map<String, String> params;
		private final String hql;
		private final List<String> sql;

		public DocQuery(String name, String comment, Map<String, String> params, String hql, String sql) {
			this(name, comment, params, hql, newList(sql));
		}

		public DocQuery(String name, String comment, Map<String, String> params, String hql, List<String> sql) {
			this.name = name;
			this.comment = comment;
			this.params = params;
			this.hql = hql;
			this.sql = sql;
		}

		public String getName() {
			return name;
		}

		public String getComment() {
			return comment;
		}

		public Map<String, String> getParams() {
			return params;
		}

		public String getHql() {
			return hql;
		}

		public List<String> getSql() {
			return sql;
		}

		private static List<String> newList(String sql) {
			List<String> result = new ArrayList<String>();
			result.add(sql);
			return  result;
		}
	}

	public static void main(String[] args) {
		try {
			final CommandLineArgs commandLineArgs = CommandLineArgs.parseCommandLineArgs( args );
			StandardServiceRegistry serviceRegistry = buildStandardServiceRegistry( commandLineArgs );
			try {
				final MetadataImplementor metadata = buildMetadata( commandLineArgs, serviceRegistry );

				Queries2DocExport queriesExport = new Queries2DocExport( serviceRegistry, metadata)
						.setOutputDirectory(commandLineArgs.outputDirectory)
						.setDelimiter( commandLineArgs.delimiter )
						.setImportSqlCommandExtractor( serviceRegistry.getService( ImportSqlCommandExtractor.class ) );
				queriesExport.execute();
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

	private static class CommandLineArgs {
		String delimiter = null;

		String outputDirectory = null;
		String propertiesFile = null;
		String cfgXmlFile = null;

		List<String> hbmXmlFiles = new ArrayList<String>();
		List<String> jarFiles = new ArrayList<String>();

		public static CommandLineArgs parseCommandLineArgs(String[] args) {
			CommandLineArgs parsedArgs = new CommandLineArgs();

			for ( String arg : args ) {
				if ( arg.startsWith( "--" ) ) {
					if ( arg.startsWith( "--output=" ) ) {
						parsedArgs.outputDirectory = arg.substring( 9 );
					}
					else if ( arg.startsWith( "--properties=" ) ) {
						parsedArgs.propertiesFile = arg.substring( 13 );
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
