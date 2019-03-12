package com.canehealth.spring.ctakes.service;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.ProcedureMention;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.store.kvstore.RotationTimeoutException;
import org.neo4j.kernel.lifecycle.LifecycleException;


@SuppressWarnings("deprecation")
public enum  Neo4jPopulation {
	   INSTANCE;

	   public static Neo4jPopulation getInstance() {
	      return INSTANCE;
	   }

	static private final Logger LOGGER = Logger.getLogger("Neo4jPopulation");

	static private final String OUTPUT_GRAPH_DB = "C:\\Users\\sanar\\Desktop\\neoctakes\\databases\\graph.db";

	private GraphDatabaseService _graphDb;
	public Node docnode;
	private JCas jcas;
	// sidestep a bug (that we can't send types across)
	// or, depending on how you look at, this issue is a Feature
	//THIS PROGRAM HAS CODE THAT DOES NOT MEET STANDARDS      
	List<String> ignoreList = Arrays.asList("medications","medication" , "problem","problems", "disease","diseases", "disorder", "sign" , /*"oral", "oral tablet",*/ "pharmaceutical preparations");

	public enum Mentions implements Label {
		document,MedicationMention,SignSymptomMention,DiseaseDisorderMention,AnatomicalSiteMention,ProcedureMention,
		MedicationFrequency,MedicationDosage,MedicationStrength,MedicationAllergy, UMLSOntologyConcept, MedicationForm, MedicationRoute;  //sorry!!!!!
	} // may be i can beautify it a little . if the code works then it deserves
	
	
	public enum relations implements RelationshipType {
		//utility
		has_mention,has_umlsontologyconcept,is_a,location_of,
		//related to Medication Mention
		has_frequency,has_form,has_Dosage,has_strength,has_allergy,has_route,
		
		//related to DiseaseDisorder
		has_alleviatingFactor, has_associatedSignSymptom
		//related to SignSymptom
		,has_bodyLocation
		//related to AnatomicalSite
		,has_bodyLaterality
		   ,has_bodySide
		//related to Procedure
		
		//related to time
		   ,has_relativeTemporalContext
	}
	
		
	public void insertNodes( JCas j) {
		this.jcas=j;
		connectToGraph(OUTPUT_GRAPH_DB);

		try (Transaction tx = _graphDb.beginTx()) {
			 docnode = _graphDb.createNode(Mentions.document);
			
			tx.success();
		}
		
		insertNodeMedication();
		insertNodeSignSymptoms();
		insertNodeProcedure();
		insertNodeAnatomicalSite();
		insertNodeDiseaseDisorder();
		insertLocationOfTextRelations();
		}	
		
		

	private void insertCommonProperties( Object o, Node node) {

		node.setProperty("hash",o.hashCode());  // ToDo (add document id/uid to avoid redundancy)
   		node.setProperty("begin", ((IdentifiedAnnotation) o).getBegin());
   		node.setProperty("end", ((IdentifiedAnnotation) o).getEnd());
   	//	TODO node.setProperty("discoverytechnique", );
   		node.setProperty("confidence",((IdentifiedAnnotation) o).getConfidence() );
   //		node.setProperty("event", ((EventMention) o).getEvent()); TODO, get confidence of event, doctime etc . get confidence in yourself too
   		node.setProperty("generic", ((IdentifiedAnnotation) o).getGeneric());
   		node.setProperty("historyof",((IdentifiedAnnotation) o).getHistoryOf());
   		
   	//	node.setProperty("originalword",((IdentifiedAnnotation) o).getCoveredText());
   		node.setProperty("polarity", ((IdentifiedAnnotation) o).getPolarity());
   		node.setProperty("uncertainty",((IdentifiedAnnotation) o).getUncertainty());  // A girl is very uncertain 
   		node.setProperty("conditional",((IdentifiedAnnotation) o).getConditional() ); 
   		
   		
   	//TODO	node.setProperty("subject", ((IdentifiedAnnotation) o).getPolarity());    //  A girl doen'nt know how to do this . yet
   		
   
		
	}
	private void insertOntologyConcenpt( Object o, Node node) {    // ABANDONED TO THE MISTS OF TIME!!!
		
		node.setProperty("code",((IdentifiedAnnotation) o).getCoveredText());
   		node.setProperty("preferredtext", ((IdentifiedAnnotation) o).getPolarity());
   		node.setProperty("score",((IdentifiedAnnotation) o).getUncertainty());
   		node.setProperty("disambiguated",((IdentifiedAnnotation) o).getConditional() );
   		node.setProperty("codingScheme", ((IdentifiedAnnotation) o).getPolarity());
   		node.setProperty("tui",((IdentifiedAnnotation) o).getUncertainty());
   		node.setProperty("cui",((IdentifiedAnnotation) o).getConditional() );
   		node.setProperty("_type",((IdentifiedAnnotation) o).getConditional() );
		
	}

	private void insertNodeMedication() {
		//connectToGraph(OUTPUT_GRAPH_DB);

		Collection<org.apache.ctakes.typesystem.type.textsem.MedicationMention> mm = JCasUtil.select(jcas,
				MedicationMention.class);
		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<MedicationMention> iterator = mm.iterator(); iterator.hasNext();) {
				MedicationMention t = iterator.next();

				Node medNode = _graphDb.createNode(Mentions.MedicationMention);
				medNode.setProperty("originalword", t.getCoveredText());
				insertCommonProperties(t, medNode);
				docnode.createRelationshipTo(medNode, relations.has_mention);
				
				// Add these as a property/attribute in future  
				//  Warning! code below sucks. bad programming . very bad sana. do it right.
				// maybe tomorrow 
				if (t.getMedicationFrequency() != null) {

					Node medfrequency = _graphDb.createNode(Mentions.MedicationFrequency);
					insertCommonProperties(t.getMedicationFrequency(), medfrequency);
					medfrequency.setProperty("value", t.getMedicationFrequency().getCategory());
					medNode.createRelationshipTo(medfrequency, relations.has_frequency);
				}

				if (t.getMedicationDosage() != null) {

					Node meddosage = _graphDb.createNode(Mentions.MedicationDosage);
					insertCommonProperties(t.getMedicationDosage(), meddosage);
					meddosage.setProperty("value", t.getMedicationDosage().getCategory());
					medNode.createRelationshipTo(meddosage, relations.has_Dosage);
				}
				
				if (t.getMedicationAllergy() != null) {

					Node medallergy = _graphDb.createNode(Mentions.MedicationAllergy);
					insertCommonProperties(t.getMedicationAllergy(), medallergy);
					medallergy.setProperty("value", t.getMedicationAllergy().getCategory());
					medNode.createRelationshipTo(medallergy, relations.has_allergy);
				}
				
				if (t.getMedicationForm() != null) {

					Node medform = _graphDb.createNode(Mentions.MedicationForm);
					insertCommonProperties(t.getMedicationForm(), medform);
					medform.setProperty("value", t.getMedicationForm().getCategory());
					medNode.createRelationshipTo(medform, relations.has_form);
				}
				
				if (t.getMedicationRoute() != null) {

					Node medroute = _graphDb.createNode(Mentions.MedicationRoute);
					insertCommonProperties(t.getMedicationRoute(), medroute);
					medroute.setProperty("value", t.getMedicationRoute().getCategory());
					medNode.createRelationshipTo(medroute, relations.has_route);
				}
				
				if (t.getMedicationStrength() != null) {

					Node mestrength = _graphDb.createNode(Mentions.MedicationStrength);
					insertCommonProperties(t.getMedicationStrength(), mestrength);
					mestrength.setProperty("value", t.getMedicationStrength().getCategory());
					medNode.createRelationshipTo(mestrength, relations.has_strength);
				}
				
				
				
				//TODO . after ccd are adjusted
				// medication frequency in ccd is baaad. need a work around
				// the whole schema of clinicalNotepreprocessor is hell
				
				
				 FSArray f = t.getOntologyConceptArr();
				 
		//		 OntologyConcept ont_array = t.getOntologyConceptArr(0);
		//		 OntologyConcept b = ont_array;
				/*
				 * FSArray ont_array = t.getOntologyConceptArr(); for
				 * (Iterator<FeatureStructure> ont_iterator = ont_array.iterator();
				 * ont_iterator.hasNext();) { Node
				 * ontNode=_graphDb.createNode(Mentions.UMLSOntologyConcept); FeatureStructure
				 * ont = ont_iterator.next(); ((FSArray) ont).get(1); JSONObject c = new
				 * JSONObject((Map) ont); int a=4;
				 * 
				 * }
				 */
			}
			tx.success();
		}
		LOGGER.info("Medication Transaction Completed Successfully");
	}
        
	
	private void insertNodeSignSymptoms() {
		//connectToGraph(OUTPUT_GRAPH_DB);
		Label lb =Mentions.SignSymptomMention;
	
		Collection<org.apache.ctakes.typesystem.type.textsem.SignSymptomMention> ss = JCasUtil.select(jcas,
				SignSymptomMention.class);
		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<SignSymptomMention> iterator = ss.iterator(); iterator.hasNext();) {

				SignSymptomMention t = iterator.next();
        //        if (t.getSegmentID()=="")
         //       	lb=DynamicLabel.label("Problem");
				Node symNode = _graphDb.createNode(lb);
				symNode.setProperty("originalword", t.getCoveredText());
				LocationOfTextRelation s = t.getBodyLocation();
				insertCommonProperties(t, symNode);
				docnode.createRelationshipTo(symNode, relations.has_mention);
				/*if(s!=null) {
					RelationArgument a = s.getArg2();
					Node relNode;
					Annotation b = a.getArgument();
					ResourceIterator<Node> nodes = _graphDb.findNodes( Mentions.AnatomicalSiteMention, "hash", b.hashCode()  );
                    	
	                	if ( nodes.hasNext() )
	                    { 
	                		relNode=nodes.next();
	                	
	                    }else {
	                    	relNode=_graphDb.createNode(Mentions.AnatomicalSiteMention);				
	    					relNode.setProperty("originalword", b.getCoveredText());
	    					insertCommonProperties(b, relNode);
		                	docnode.createRelationshipTo(relNode, relations.has_mention);
	                    }
	                	
					symNode.createRelationshipTo(relNode, relations.has_bodyLocation);
				
				}*/

			}

			tx.success();
		}
		
		// TODO need to add these alaa blaa yet
 /*//alleviatingFactor bodyLaterality
		alleviatingFactor: <null>
		   bodyLaterality: <null>
		   bodySide: <null>
		   bodyLocation: <null>
		   course: <null>
		   duration: <null>
		   endTime: <null>
		   exacerbatingFactor: <null>
		   severity: <null>
		   startTime: <null>
		   relativeTemporalContext:
		   Event:contextualModality: <null>
         contextualAspect: <null>
         permanence: <null>
         category: <null>
         aspect: <null>
         docTimeRel: "OVERLAP"
         degree: <null>
         polarity: 0
         Disease:alleviatingFactor: <null>
   associatedSignSymptom: <null>
   bodyLaterality: <null>
   bodySide: <null>
   bodyLocation: <null>
   course: <null>
   duration: <null>
   endTime: <null>
   exacerbatingFactor: <null>
   startTime: <null>
   relativeTemporalContext: <null>
   severity:
          */
		LOGGER.info("Sign/Symptoms Transaction Completed Successfully");
	}
	
	
	
	
	private void insertNodeAnatomicalSite() {
		// connectToGraph(OUTPUT_GRAPH_DB);

		Collection<org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention> as = JCasUtil.select(jcas,
				AnatomicalSiteMention.class);
		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<AnatomicalSiteMention> iterator = as.iterator(); iterator.hasNext();) {

				AnatomicalSiteMention t = iterator.next();

				Node siteNode = _graphDb.createNode(Mentions.AnatomicalSiteMention);
				ResourceIterator<Node> nodes = _graphDb.findNodes(Mentions.AnatomicalSiteMention, "hash", t.hashCode());
				   if (nodes.hasNext()) {
						LOGGER.info("found a node with hash" + t.hashCode());
						continue;
					}
				
				siteNode.setProperty("originalword", t.getCoveredText());
				insertCommonProperties(t, siteNode);
				docnode.createRelationshipTo(siteNode, relations.has_mention);
			}

			tx.success();
		}

		LOGGER.info("Anatomical Site Transaction Completed Successfully");
	}
	
	private void insertNodeProcedure() {
		//connectToGraph(OUTPUT_GRAPH_DB);

		Collection<org.apache.ctakes.typesystem.type.textsem.ProcedureMention> p = JCasUtil.select(jcas,
				ProcedureMention.class);
		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<ProcedureMention> iterator = p.iterator(); iterator.hasNext();) {

				ProcedureMention t = iterator.next();

				Node procNode = _graphDb.createNode(Mentions.ProcedureMention);
				procNode.setProperty("originalword", t.getCoveredText());
				insertCommonProperties(t, procNode);
				docnode.createRelationshipTo(procNode, relations.has_mention);
			}

			tx.success();
		}

		LOGGER.info("Procedure Transaction Completed Successfully");
	}
	
	private void insertNodeDiseaseDisorder() {
		//connectToGraph(OUTPUT_GRAPH_DB);

		Collection<org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention> dd = JCasUtil.select(jcas,
				DiseaseDisorderMention.class);
		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<DiseaseDisorderMention> iterator = dd.iterator(); iterator.hasNext();) {

				DiseaseDisorderMention t = iterator.next();

				Node disNode = _graphDb.createNode(Mentions.DiseaseDisorderMention);
				disNode.setProperty("originalword", t.getCoveredText());
				insertCommonProperties(t, disNode);
				docnode.createRelationshipTo(disNode, relations.has_mention);
			}

			tx.success();
		}

		LOGGER.info("DiseaseDisorder Transaction Completed Successfully");
	}

	/*Magic. Do not Touch
	 *  but the "real" solution is much more complicated
	 */
	 
	private void insertLocationOfTextRelations() {
		Collection<org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation>loc_of=JCasUtil.select(jcas, LocationOfTextRelation.class);
		
		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<LocationOfTextRelation> iterator = loc_of.iterator(); iterator.hasNext();) {
					
				LocationOfTextRelation t = iterator.next();
				
				Node a1node,a2node;

				RelationArgument arg1 = t.getArg1();
				Annotation a1 = arg1.getArgument();
				
				arg1.getType();
				String classname = a1.getClass().getName();
				int pos = classname.lastIndexOf ('.') + 1; 
				String class1 = classname.substring(pos);
			
								
				Label label1 = DynamicLabel.label(class1);
				
				 ResourceIterator<Node> nodes = _graphDb.findNodes( label1, "hash", a1.hashCode()  );
	                               	
	                	if ( nodes.hasNext() )
	                    { 
	                		a1node=nodes.next();
	                	
	                    }else {
	                    	a1node=_graphDb.createNode(label1);
		                	a1node.setProperty("originalword", a1.getCoveredText());
		                	insertCommonProperties(a1, a1node);
		                	docnode.createRelationshipTo(a1node, relations.has_mention);
	                    }
	                                	
	                 
				RelationArgument arg2 = t.getArg2();
				Annotation a2 = arg2.getArgument();
				
				classname = a2.getClass().getName();
				pos = classname.lastIndexOf ('.') + 1; 
				String class2 = classname.substring(pos);
				Label label2 = DynamicLabel.label(class2);
				 
				ResourceIterator<Node> nodes2 = _graphDb.findNodes( label2, "hash", a2.hashCode()  );
	                	                	
	                	if ( nodes2.hasNext() )
	                    { 
	                	   a2node=nodes2.next();
	                    	
	                    }
	                	else {
	                		a2node=_graphDb.createNode(label2);
		                	a2node.setProperty("originalword", a2.getCoveredText());
		                	insertCommonProperties(a2, a2node);
		                	docnode.createRelationshipTo(a1node, relations.has_mention);
	                	}
	                	
	                	 a1node.createRelationshipTo(a2node, relations.location_of);	                     
				}
			tx.success();
			}

		LOGGER.info("locationOftextRelation Transaction Completed Successfully"); //huh
		
		}
		
	
	
	
	   private void createOutputGraph( final String outputDbPath ) {
	      final File outputGraph = new File( outputDbPath );
	      final String path = outputGraph.getAbsolutePath();
	      if ( !outputGraph.exists() ) {
	    	  LOGGER.info( "Creating output Graph " + path );
	    	  outputGraph.mkdirs();
	        
	      } else {
	    	
	    	  LOGGER.info( "Replacing existing output Graph " + path );
		         deleteDirectory( outputGraph );
	      }
	    
	   }

	   static private boolean deleteDirectory( final File directory ) {
	      final File[] files = directory.listFiles();
	      if ( files == null ) {
	         return true;
	      }
	      boolean ok = true;
	      for ( File file : files ) {
	         if ( file.isDirectory() ) {
	            ok &= deleteDirectory( file );
	         }
	         ok &= file.delete();
	      }
	      return ok;
	   }


	   public String getOutputGraphDb() {
	      return OUTPUT_GRAPH_DB;
	   }

	   public GraphDatabaseService connectToGraph() {
		   
	      return connectToGraph( getOutputGraphDb() );
	   }

	    public GraphDatabaseService connectToGraph( final String graphDbPath ) {
	      if ( _graphDb != null ) {
	         return _graphDb;
	      }
	      createOutputGraph( graphDbPath );
	      final File graphDbFile = new File( graphDbPath );
	      if ( !graphDbFile.isDirectory() ) {
	         LOGGER.error( "No Database exists at: " + graphDbPath );
	         System.exit( -1 );
	      }
	      _graphDb = new GraphDatabaseFactory()
	      .newEmbeddedDatabase( graphDbFile );
	      if ( !_graphDb.isAvailable( 500 ) ) {
	         LOGGER.error( "Could not initialize neo4j connection for: " + graphDbPath );
	         System.exit( -1 );
	      }
	   //   registerShutdownHook( _graphDb );
	      return _graphDb;
	   }

	   public GraphDatabaseService getGraph() {
	      if ( _graphDb == null ) {
	         return connectToGraph();
	      }
	      return _graphDb;
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


	}

//If you're reading this, then my program is probably a success
//Hahah just kidding :D 
	

