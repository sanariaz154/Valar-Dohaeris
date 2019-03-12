package com.canehealth.spring.ctakes.service;

import org.apache.log4j.Logger;
import org.apache.uima.jcas.JCas;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.Values;

public class Neo4jDriverPopulation  {

	//private final Driver driver;
	static private final Logger LOGGER = Logger.getLogger("Neo4jPopulation");
    public Neo4jDriverPopulation(  )
    { 
    	String statement =                                                        
    		      "  CREATE (p:Person) SET p.id = $id,p.firstname=firstname";
    	Value params = Values.parameters("id", "id", "firstName", "firstname");
    	Driver driver = GraphDatabase.driver("bolt://localhost:11001", AuthTokens.basic("neo4j", "1"));
    	try (Session session = driver.session()) {
    		 try (Transaction tx = session.beginTransaction()) {   
    	    StatementResult rs = tx.run(statement, params);
    	    tx.success();                                                         
    	  tx.close();
    	}}
    		 
    	driver.close();
    	LOGGER.info("done ");
    }
    
}
