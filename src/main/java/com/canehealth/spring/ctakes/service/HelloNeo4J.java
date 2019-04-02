package com.canehealth.spring.ctakes.service;

import java.io.File;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.store.kvstore.RotationTimeoutException;
import org.neo4j.kernel.lifecycle.LifecycleException;

public class HelloNeo4J implements AutoCloseable {
	
	
	public enum Tutorials implements Label {
		JAVA,SCALA,SQL,NEO4J;
	}
	
	public enum TutorialRelationships implements RelationshipType{
		JVM_LANGIAGES,NON_JVM_LANGIAGES;
	}
   public static void smh() {
	   File storeDir= new File("C:\\Users\\sanar\\Desktop\\neotemp1\\database\\graph.db");
	   GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
	   GraphDatabaseService db= dbFactory.newEmbeddedDatabase(storeDir);
	   
	   try (Transaction tx = db.beginTx()) {
			// Perform DB operations
		   
		   Node javaNode = db.createNode(Tutorials.JAVA);
		   Node scalaNode = db.createNode(Tutorials.SCALA);
		   
		   javaNode.setProperty("TutorialID", "JAVA001");
		   javaNode.setProperty("Title", "Learn Java");
		   javaNode.setProperty("NoOfChapters", "25");
		   javaNode.setProperty("Status", "Completed");	
		   	
		   scalaNode.setProperty("TutorialID", "SCALA001");
		   scalaNode.setProperty("Title", "Learn Scala");
		   scalaNode.setProperty("NoOfChapters", "20");
		   scalaNode.setProperty("Status", "Completed");
		   
		   
		   Relationship relationship = javaNode.createRelationshipTo(scalaNode,
					TutorialRelationships.JVM_LANGIAGES);
		   
		   relationship.setProperty("Id","1234");
		   relationship.setProperty("OOPS","YES");
		   relationship.setProperty("FP","YES");
		   
			tx.success();
		}
	   
	   
	//   registerShutdownHook(db);
	   System.out.println("Done successfully");
   }
   
   static private void registerShutdownHook( final GraphDatabaseService graphDb ) {
	      // Registers a shutdown hook for the Neo4j instance so that it
	      // shuts down nicely when the VM exits (even if you "Ctrl-C" the
	      // running application).
	      Runtime.getRuntime().addShutdownHook( new Thread( () -> {
	         try {
	            graphDb.shutdown();
	         } catch ( LifecycleException | RotationTimeoutException multE ) {
	            // ignore
	         }
	      } ) );
	   }

@Override
public void close() throws Exception {
	// TODO Auto-generated method stub
	
}
}
