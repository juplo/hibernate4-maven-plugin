Hibernate-4 maven plugin Envers sample project
====================

Introduction
---------------------

This is a sample project that can be used to demonstrate the [Hibernate4-maven-plugin](http://juplo.de/hibernate4-maven-plugin/)
in combination with the Hibernate Envers module.

The Maven pom.xml file contains the definitions to run the Hibernate4-maven-plugin.

The project code contains a JPA entity, an Envers audit revision entity and
a single integration test using an in-memory HSQLDB database.

The integration test performs the following actions:

*   Initialize a JTA environment using [Atomikos transaction essentials](http://www.atomikos.com/Main/TransactionsEssentials/)
*   Startup an XA datasource
*   Run the SQL-script to drop any existing tables in the HSQLDB database (script/drop-tables-hsqldb.sql)
*   Run the SQL-script to create the tables in the HSQLDB database (create-tables-hsqldb.sql)

        Note: this script is created by the Hibernate4-maven-plugin

* Load the Hibernate (and Envers) configuration, including the validation of the database schema
* Persist and update entities that are audited by Hibernate Envers
* Verify the revisions and the audit tables

Usage
---------------------

__Rebuild the SQL-script using the Hibernate4-maven-plugin to create the database__

    mvn -PcreateHsqlDbScript clean compile hibernate4:export

__Build and run the integration tests__

    mvn clean package

