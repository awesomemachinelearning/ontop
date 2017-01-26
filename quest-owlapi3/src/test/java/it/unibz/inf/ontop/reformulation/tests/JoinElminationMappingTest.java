package it.unibz.inf.ontop.reformulation.tests;

/*
 * #%L
 * ontop-quest-owlapi3
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
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
 * #L%
 */

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import it.unibz.inf.ontop.injection.QuestConfiguration;
import it.unibz.inf.ontop.owlrefplatform.owlapi.*;
import junit.framework.TestCase;

import it.unibz.inf.ontop.owlrefplatform.core.QuestConstants;
import it.unibz.inf.ontop.injection.QuestCoreSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The following tests take the Stock exchange scenario and execute the queries
 * of the scenario to validate the results. The validation is simple, we only
 * count the number of distinct tuples returned by each query, which we know in
 * advance.
 * 
 * We execute the scenario in different modes, virtual, classic, with and
 * without optimizations.
 * 
 * The data is obtained from an inmemory database with the stock exchange
 * tuples. If the scenario is run in classic, this data gets imported
 * automatically by the reasoner.
 */
public class JoinElminationMappingTest extends TestCase {
	private Connection conn;

	private Logger log = LoggerFactory.getLogger(this.getClass());

	final String owlfile = "src/test/resources/test/ontologies/scenarios/join-elimination-test.owl";
	final String obdafile = "src/test/resources/test/ontologies/scenarios/join-elimination-test.obda";

	String url = "jdbc:h2:mem:questjunitdb";
	String username = "sa";
	String password = "";

	@Override
	public void setUp() throws Exception {
		// String driver = "org.h2.Driver";

		conn = DriverManager.getConnection(url, username, password);
		Statement st = conn.createStatement();

		String createStr = 
				"CREATE TABLE address (" + "id integer NOT NULL," + "street character varying(100),"
				+ "number integer," + "city character varying(100)," + "state character varying(100),"
				+ "country character varying(100), PRIMARY KEY(id)" + ");";

		st.executeUpdate(createStr);
		conn.commit();
	}

	@Override
	public void tearDown() throws Exception {
			dropTables();
			conn.close();
	}

	private void dropTables() throws SQLException, IOException {
		Statement st = conn.createStatement();
		st.executeUpdate("DROP TABLE address;");
		st.close();
		conn.commit();
	}
	
	private void runTests(Properties p) throws Exception {
		// Creating a new instance of the reasoner
		QuestOWLFactory factory = new QuestOWLFactory();
		QuestConfiguration configuration;
		if (p.getProperty(QuestCoreSettings.ABOX_MODE).equals(QuestConstants.VIRTUAL) ||
				Boolean.getBoolean(p.getProperty(QuestCoreSettings.OBTAIN_FROM_MAPPINGS))) {

			configuration = QuestConfiguration.defaultBuilder()
					.nativeOntopMappingFile(obdafile)
					.ontologyFile(owlfile)
					.properties(p)
					.jdbcUrl(url)
					.jdbcUser(username)
					.jdbcPassword(password)
					.build();
		}
		else {
			configuration = QuestConfiguration.defaultBuilder()
					.ontologyFile(owlfile)
					.properties(p)
					.build();
		}

		QuestOWL reasoner = factory.createReasoner(configuration);
		reasoner.flush();

		// Now we are ready for querying
		QuestOWLStatement st = reasoner.getStatement();

		boolean fail = false;

		String query = 
				"PREFIX : <http://it.unibz.krdb/obda/ontologies/join-elimination-test.owl#> \n" +
				"SELECT ?x WHERE {?x :R ?y. ?y a :A}";
		try {
			System.out.println("\n\nSQL:\n" + st.getExecutableQuery(query));
			QuestOWLResultSet rs = st.executeTuple(query);
			rs.nextRow();
		} catch (Exception e) {
			log.debug(e.getMessage(), e);
			fail = true;
		}
		/* Closing resources */
		st.close();
		reasoner.dispose();

		/* Comparing and printing results */
		assertFalse(fail);
	}

//	public void testSiEqSig() throws Exception {
//		Properties p  = new Properties();
//		p.setProperty(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
//		p.setProperty(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
//		p.setProperty(QuestPreferences.OPTIMIZE_TBOX_SIGMA, "true");
//		p.setProperty(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
//		p.setProperty(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "false");
//		p.setProperty(QuestPreferences.DBTYPE, QuestConstants.SEMANTIC_INDEX);
//		runTests(new QuestPreferences(p));
//	}
//
//	public void testSiEqNoSig() throws Exception {
//		Properties p  = new Properties();
//		p.setProperty(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
//		p.setProperty(QuestPreferences.OPTIMIZE_EQUIVALENCES, "true");
//		p.setProperty(QuestPreferences.OPTIMIZE_TBOX_SIGMA, "false");
//		p.setProperty(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
//		p.setProperty(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "false");
//		p.setProperty(QuestPreferences.DBTYPE, QuestConstants.SEMANTIC_INDEX);
//		runTests(new QuestPreferences(p));
//	}
//
//	public void testSiNoEqSig() throws Exception {
//		Properties p  = new Properties();
//		p.setProperty(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
//		p.setProperty(QuestPreferences.OPTIMIZE_EQUIVALENCES, "false");
//		p.setProperty(QuestPreferences.OPTIMIZE_TBOX_SIGMA, "true");
//		p.setProperty(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
//		p.setProperty(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "false");
//		p.setProperty(QuestPreferences.DBTYPE, QuestConstants.SEMANTIC_INDEX);
//		runTests(new QuestPreferences(p));
//	}
//
//	public void testSiNoEqNoSig() throws Exception {
//		Properties p  = new Properties();
//		p.setProperty(QuestPreferences.ABOX_MODE, QuestConstants.CLASSIC);
//		p.setProperty(QuestPreferences.OPTIMIZE_EQUIVALENCES, "false");
//		p.setProperty(QuestPreferences.OPTIMIZE_TBOX_SIGMA, "false");
//		p.setProperty(QuestPreferences.OBTAIN_FROM_MAPPINGS, "true");
//		p.setProperty(QuestPreferences.OBTAIN_FROM_ONTOLOGY, "false");
//		p.setProperty(QuestPreferences.DBTYPE, QuestConstants.SEMANTIC_INDEX);
//		runTests(new QuestPreferences(p));
//	}

	/*
	 * Direct Mapping
	 */
	public void disabletestDiEqSig() throws Exception {
		Properties p  = new Properties();
		p.setProperty(QuestCoreSettings.ABOX_MODE, QuestConstants.CLASSIC);
		p.setProperty(QuestCoreSettings.OPTIMIZE_EQUIVALENCES, "true");
		p.setProperty(QuestCoreSettings.OBTAIN_FROM_MAPPINGS, "true");
		p.setProperty(QuestCoreSettings.OBTAIN_FROM_ONTOLOGY, "false");
		p.setProperty(QuestCoreSettings.DBTYPE, QuestConstants.DIRECT);
		runTests(p);
	}

	public void disabletestDiEqNoSig() throws Exception {
		Properties p  = new Properties();
		p.setProperty(QuestCoreSettings.ABOX_MODE, QuestConstants.CLASSIC);
		p.setProperty(QuestCoreSettings.OPTIMIZE_EQUIVALENCES, "true");
		p.setProperty(QuestCoreSettings.OBTAIN_FROM_MAPPINGS, "true");
		p.setProperty(QuestCoreSettings.OBTAIN_FROM_ONTOLOGY, "false");
		p.setProperty(QuestCoreSettings.DBTYPE, QuestConstants.DIRECT);
		runTests(p);
	}

	/**
	 * This is a very slow test, disable it if you are doing routine checks.
	 * 
	 * @throws Exception
	 */
	public void disabletestDiNoEqSig() throws Exception {
		Properties p  = new Properties();
		p.setProperty(QuestCoreSettings.ABOX_MODE, QuestConstants.CLASSIC);
		p.setProperty(QuestCoreSettings.OPTIMIZE_EQUIVALENCES, "false");
		p.setProperty(QuestCoreSettings.OBTAIN_FROM_MAPPINGS, "true");
		p.setProperty(QuestCoreSettings.OBTAIN_FROM_ONTOLOGY, "false");
		p.setProperty(QuestCoreSettings.DBTYPE, QuestConstants.DIRECT);
		runTests(p);
	}

	/**
	 * This is a very slow test, disable it if you are doing routine checks.
	 * 
	 * @throws Exception
	 */
	public void disabletestDiNoEqNoSig() throws Exception {
		Properties p  = new Properties();
		p.setProperty(QuestCoreSettings.ABOX_MODE, QuestConstants.CLASSIC);
		p.setProperty(QuestCoreSettings.OPTIMIZE_EQUIVALENCES, "false");
		p.setProperty(QuestCoreSettings.OBTAIN_FROM_MAPPINGS, "true");
		p.setProperty(QuestCoreSettings.OBTAIN_FROM_ONTOLOGY, "false");
		p.setProperty(QuestCoreSettings.DBTYPE, QuestConstants.DIRECT);
		runTests(p);
	}

	public void testViEqSig() throws Exception {
		Properties p  = new Properties();
		p.setProperty(QuestCoreSettings.ABOX_MODE, QuestConstants.VIRTUAL);
		p.setProperty(QuestCoreSettings.OPTIMIZE_EQUIVALENCES, "true");

		runTests(p);
	}

	public void testViEqNoSig() throws Exception {
		Properties p  = new Properties();
		p.setProperty(QuestCoreSettings.ABOX_MODE, QuestConstants.VIRTUAL);
		p.setProperty(QuestCoreSettings.OPTIMIZE_EQUIVALENCES, "true");
		runTests(p);
	}

	/**
	 * This is a very slow test, disable it if you are doing routine checks.
	 */
	public void testViNoEqSig() throws Exception {
		Properties p  = new Properties();
		p.setProperty(QuestCoreSettings.ABOX_MODE, QuestConstants.VIRTUAL);
		p.setProperty(QuestCoreSettings.OPTIMIZE_EQUIVALENCES, "false");
		runTests(p);
	}

	/**
	 * This is a very slow test, disable it if you are doing routine checks.
	 */
	public void testViNoEqNoSig() throws Exception {
		Properties p  = new Properties();
		p.setProperty(QuestCoreSettings.ABOX_MODE, QuestConstants.VIRTUAL);
		p.setProperty(QuestCoreSettings.OPTIMIZE_EQUIVALENCES, "false");
		runTests(p);
	}
}
