package com.canehealth.spring.ctakes.service;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.apache.ctakes.relationextractor.eval.SHARPXMI.DocumentIDAnnotator;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.DegreeOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.ProcedureMention;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
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

import com.canehealth.spring.ctakes.service.Neo4jCDAPopulation.Mentions;
import com.canehealth.spring.ctakes.service.Neo4jCDAPopulation.relations;


@SuppressWarnings("deprecation")
public enum  Neo4jPopulation2 {
	   INSTANCE;

	   public static Neo4jPopulation2 getInstance() {
	      return INSTANCE;
	   }

	   static private final Logger LOGGER = Logger.getLogger("Neo4jPopulation2");

		static private final String OUTPUT_GRAPH_DB = "C:\\Users\\sanar\\Desktop\\neoctakes\\databases\\graph.db";

		private GraphDatabaseService _graphDb;
		public Node docnode;
		private JCas jcas;
		private ResourceIterator<Node> nodes, nodes2 ;

		List<String> ignoreList = Arrays.asList("history","condition",/*"anterior",*/"medications","medication" , "problem", "disease", "disorder", "sign" , /*"oral", "oral tablet",*/ "pharmaceutical preparations");

		public enum Mentions implements Label {
			document,MedicationMention,SignSymptomMention,DiseaseDisorderMention,AnatomicalSiteMention,ProcedureMention,
			Modifier,TimeMention,MedicationFrequency,MedicationDosage,MedicationStrength,MedicationAllergy, UMLSConcept, MedicationForm, MedicationRoute;  //sorry!!!!!
		
		} // may be i can beautify it a little . if the code works then it deserves
		
		
		public enum relations implements RelationshipType {
			//utility
			has_mention,has_umlsontologyconcept,is_a,location_of,degree_of,
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
			   ,temporal_relation  ,has_relativeTemporalContext
		}
		String doc_id;
			
		public int insertNodes( JCas j) throws CASException {
			this.jcas=j;
			connectToGraph(OUTPUT_GRAPH_DB);

			try (Transaction tx = _graphDb.beginTx()) {
				 
				doc_id = JCasUtil.select(jcas,DocumentID.class).iterator().next().getDocumentID();
				nodes = _graphDb.findNodes( Mentions.document, "doc-id", doc_id );
            	if ( nodes.hasNext() )
            		
            	{
            		return 0;
            	}
            		
				docnode = _graphDb.createNode(Mentions.document);
				 docnode.setProperty("doc-id", doc_id);
				// j=jcas.getView("UriView");
			//	 String s = jcas.getSofaDataURI();
				 
				 docnode.setProperty("mime/type",jcas.getSofaMimeType());
			//	 docnode.setProperty("doc-uri",jcas.getView("UriView").sofaURI());

				tx.success();
			}
	//		jcas.getViewIterator();
			
		//	insertTimeMentions();
			
			
			insertNodeMedication();
			insertNodeSignSymptoms();
			insertNodeProcedure();
			insertNodeAnatomicalSite();
			insertNodeDiseaseDisorder();
			insertDegreeOfTextRelations();
			insertLocationOfTextRelations();
			
			return 1;
			}	
			
		
		private String createID( Object o) {
			
		return  String.valueOf(((IdentifiedAnnotation) o).getBegin()) +"-"+ String.valueOf(((IdentifiedAnnotation) o).getEnd()); //+ String.valueOf(doc_id);
			//return A+B+String.valueOf(doc_id);
		}
	
		
		private void insertCommonProperties( Object o, Node node) {
			node.setProperty("uid",createID(o));
			node.setProperty("hash",o.hashCode());
	   		//node.setProperty("begin", ((IdentifiedAnnotation) o).getBegin());   //we dont need begin and end- if needed, can be get through uid of node
	   		//node.setProperty("end", ((IdentifiedAnnotation) o).getEnd());
	   	//	TODO node.setProperty("discoverytechnique", );
	   		node.setProperty("confidence",((IdentifiedAnnotation) o).getConfidence() );
	   //		node.setProperty("event", ((EventMention) o).getEvent()); TODO, get confidence of event, doctime etc . get confidence in yourself too
	   		node.setProperty("generic", ((IdentifiedAnnotation) o).getGeneric());
	   		node.setProperty("historyof",((IdentifiedAnnotation) o).getHistoryOf());
	   		
	   	//	node.setProperty("originalword",((IdentifiedAnnotation) o).getCoveredText());
	   		node.setProperty("polarity", ((IdentifiedAnnotation) o).getPolarity());
	   		node.setProperty("uncertainty",((IdentifiedAnnotation) o).getUncertainty());  // A girl is very uncertain 
	   		node.setProperty("conditional",((IdentifiedAnnotation) o).getConditional() ); 
	   		
	   		
	     	node.setProperty("subject", Optional.ofNullable(((IdentifiedAnnotation) o).getSubject()).orElse("N/A"));    //  A girl doen'nt know how to do this . yet
	   		
	   
			
		}
		
		private void insertEventProperties(Event e , Node node) {
			
			//try/catch???
		   	//	node.setProperty("uncertainty",e.getUncertainty());  // A girl is very uncertain 
		   	//	node.setProperty("conditional",e.getConditional() );
			
			node.setProperty("event-subject", Optional.ofNullable(e.getSubject()).orElse("N/A"));
			node.setProperty("event-hashcode",e.hashCode() );
			node.setProperty("event-polarity", e.getPolarity());
			node.setProperty("event-confidence",e.getConfidence() );	
			node.setProperty("event-docTimeRel", Optional.ofNullable(e.getProperties().getDocTimeRel()).orElse("N/A"));
			node.setProperty("event-catogary", Optional.ofNullable(e.getProperties().getCategory()).orElse("N/A"));
			node.setProperty("event-aspect",Optional.ofNullable(e.getProperties().getAspect()).orElse("N/A"));
			node.setProperty("event-permanence",Optional.ofNullable(e.getProperties().getAspect()).orElse("N/A"));
			node.setProperty("event-contextualAspect",Optional.ofNullable(e.getProperties().getContextualAspect() ).orElse("N/A"));
			node.setProperty("event-contextualModality",Optional.ofNullable(e.getProperties().getContextualModality() ).orElse("N/A"));
			node.setProperty("event-degree",Optional.ofNullable(e.getProperties().getDegree()).orElse("N/A") );

			

			
			
		}
		private void insertOntologyConcenpt(UmlsConcept umls_ont , Node node) {    // ABANDONED TO THE MISTS OF TIME!!!
			
	
			Node ontNode=_graphDb.createNode(Mentions.UMLSConcept);
			
			ontNode.setProperty("code",umls_ont.getCode());
			ontNode.setProperty("preferredtext", umls_ont.getPreferredText());
			ontNode.setProperty("score",umls_ont.getScore());
			ontNode.setProperty("disambiguated",umls_ont.getDisambiguated() );
			ontNode.setProperty("codingScheme", umls_ont.getCodingScheme());
			ontNode.setProperty("tui",umls_ont.getTui());
			ontNode.setProperty("cui",umls_ont.getCui() );
			ontNode.setProperty("oid",umls_ont.getOid() );
	   		node.createRelationshipTo(ontNode, relations.has_umlsontologyconcept);
		}

		private void insertNodeMedication() {
		

			Collection<org.apache.ctakes.typesystem.type.textsem.MedicationMention> mm = JCasUtil.select(jcas,
					MedicationMention.class);
			try (Transaction tx = _graphDb.beginTx()) {
				for (Iterator<MedicationMention> iterator = mm.iterator(); iterator.hasNext();) {
					MedicationMention t = iterator.next();
					t.getSegmentID();
					t.getCoveredText();
				    t.getOntologyConceptArr();
					if(ignoreList.contains((t.getCoveredText()).toLowerCase()) || !((t.getSegmentID()).equals("2.16.840.1.113883.10.20.22.2.1.1") || (t.getSegmentID()).equals("10160-0")))
						continue;
					Node medNode = _graphDb.createNode(Mentions.MedicationMention);
					medNode.setProperty("originalword", t.getCoveredText());
					insertCommonProperties(t, medNode);
					docnode.createRelationshipTo(medNode, relations.has_mention);
					
					// Add these as a property/attribute in future  
					//  Warning! code below sucks. bad programming . very bad sana. do it right.
					// maybe tomorrow 
					t.getMedicationDuration();
					
					
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
					
					
				
					 insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0),medNode);
					 insertEventProperties(t.getEvent(), medNode);
			
					
				}
				tx.success();
			}
			LOGGER.info("Medication Transaction Completed Successfully");
		}
	        

		private void insertNodeSignSymptoms() {
			//connectToGraph(OUTPUT_GRAPH_DB);
			//added	//1.3.6.1.4.1.19376.1.5.3.1.3.4	//10164-2   history of patient illness
			//        problem list 2.16.840.1.113883.10.20.22.2.5.1 ,11450-4
			//added		//2.16.840.1.113883.10.20.22.2.41,8653-8,HOSPITAL DISCHARGE INSTRUCTIONS,Discharge Instructions,	Written discharge instructions
			//added		//2.16.840.1.113883.10.20.22.2.24,11535-2,Hospital Discharge Diagnosis,discharge diagnosis,FINAL DIAGNOSIS
			//added	   //	2.16.840.1.113883.10.20.22.2.16,11493-4,Hospital Discharge Studies Summary
			//added	// 2.16.840.1.113883.10.20.22.2.4.1,8716-3,Vital Signs
			//added	//2.16.840.1.113883.10.20.22.2.13,46239-0,Chief Complaint and Reason for Visit
			//added	//1.3.6.1.4.1.19376.1.5.3.1.1.13.2.1,10154-3,CHIEF COMPLAINT,admit diagnosis,principal discharge diagnosis,principal diagnosis,principal diagnoses,secondary diagnosis,other medical issues considered at this time 
			//      //family history
			//      //2.16.840.1.113883.10.20.2.10,29545-1,PHYSICAL EXAMINATION,physical exam

			List<String> symptomsectionlist = Arrays.asList("1.3.6.1.4.1.19376.1.5.3.1.3.4","10164-2" , "2.16.840.1.113883.10.20.22.2.4.1", "8716-3", "2.16.840.1.113883.10.20.22.2.13", "46239-0", "1.3.6.1.4.1.19376.1.5.3.1.1.13.2.1", "10154-3" );
			List<String> futuresymptomsectionlist = Arrays.asList("2.16.840.1.113883.10.20.22.2.41","8653-8" , "2.16.840.1.113883.10.20.22.2.24", "11535-2" );
			List<String> futuresymptomsectionlist2 = Arrays.asList("2.16.840.1.113883.10.20.22.2.16", "11493-4" );

			Label lb ;
			Collection<org.apache.ctakes.typesystem.type.textsem.SignSymptomMention> ss = JCasUtil.select(jcas,
					SignSymptomMention.class);
			try (Transaction tx = _graphDb.beginTx()) {
				for (Iterator<SignSymptomMention> iterator = ss.iterator(); iterator.hasNext();) {

					SignSymptomMention t = iterator.next();
					t.getSegmentID();
			
					if(ignoreList.contains((t.getCoveredText()).toLowerCase()))
						continue;	
					if ((t.getSegmentID()).equals("11450-4"))
						lb=Label.label("Problem");
					else if (symptomsectionlist.contains((t.getSegmentID()).toLowerCase()))
						lb=Mentions.SignSymptomMention;
					else if (futuresymptomsectionlist.contains((t.getSegmentID()).toLowerCase()))	
						lb=Label.label("FutureSymptoms");
					else if (futuresymptomsectionlist2.contains((t.getSegmentID()).toLowerCase()))	
						lb=Label.label("FutureSymptoms");
					else continue;
				
					t.getCoveredText();
					t.getAlleviatingFactor();
					t.getSeverity();
					t.getEvent().getProperties().getDegree();
				
					String	id=t.getSegmentID();
				     
						Node symNode = _graphDb.createNode(lb);
					symNode.setProperty("originalword", t.getCoveredText());
					LocationOfTextRelation s = t.getBodyLocation();
					insertCommonProperties(t, symNode);
					insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0),symNode);
					insertEventProperties(t.getEvent(), symNode);
					docnode.createRelationshipTo(symNode, relations.has_mention);
					
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
					if(ignoreList.contains((t.getCoveredText()).toLowerCase()))
						continue;
					Node siteNode = _graphDb.createNode(Mentions.AnatomicalSiteMention);
					
					//why am i doing this check after creating the node ?? too scared to change it now
					ResourceIterator<Node> nodes = _graphDb.findNodes(Mentions.AnatomicalSiteMention, "uid", createID(t));
					   if (nodes.hasNext()) {
							LOGGER.info("found a node with hash" + t.hashCode());
							continue;
						}
					
					siteNode.setProperty("originalword", t.getCoveredText());
					insertCommonProperties(t, siteNode);
					insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0),siteNode);
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
					 insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0),procNode);
					 insertEventProperties(t.getEvent(), procNode);
			//		docnode.createRelationshipTo(procNode, relations.has_mention);
				}

				tx.success();
			}

			LOGGER.info("Procedure Transaction Completed Successfully");
		}
		
		private void insertNodeDiseaseDisorder() {
			//connectToGraph(OUTPUT_GRAPH_DB);
			Label lb =Mentions.DiseaseDisorderMention;
					Collection<org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention> dd = JCasUtil.select(jcas,
							DiseaseDisorderMention.class);
			try (Transaction tx = _graphDb.beginTx()) {
				for (Iterator<DiseaseDisorderMention> iterator = dd.iterator(); iterator.hasNext();) {

					DiseaseDisorderMention t = iterator.next();
					if(ignoreList.contains((t.getCoveredText()).toLowerCase()))
						continue;
					if ((t.getSegmentID()).equals("11450-4"))
						lb=Label.label("Problem");
					Node disNode = _graphDb.createNode(lb);
					disNode.setProperty("originalword", t.getCoveredText());
					insertCommonProperties(t, disNode);
					 insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0),disNode);
					 insertEventProperties(t.getEvent(), disNode);
					docnode.createRelationshipTo(disNode, relations.has_mention);
				}

				tx.success();
			}

			LOGGER.info("DiseaseDisorder Transaction Completed Successfully");
		}
		
		


		
	//TODO	//PTP: if event mentions are note suppose to record in graph, why creating new nodes here?? 
		private void insertDegreeOfTextRelations() {
			Collection<DegreeOfTextRelation>deg_of=JCasUtil.select(jcas, DegreeOfTextRelation.class);
			try (Transaction tx = _graphDb.beginTx()) {
				for (Iterator<DegreeOfTextRelation> iterator = deg_of.iterator(); iterator.hasNext();) {
						
					DegreeOfTextRelation t = iterator.next();
					
					Node a1node,a2node;

					RelationArgument arg1 = t.getArg1();
					Annotation a1 = arg1.getArgument();
					
					arg1.getType();
					String classname = a1.getClass().getName();
					int pos = classname.lastIndexOf ('.') + 1; 
					String class1 = classname.substring(pos);
				    
				    
				    
				    RelationArgument arg2 = t.getArg2();
					Annotation a2 = arg2.getArgument();
					
					classname = a2.getClass().getName();
					pos = classname.lastIndexOf ('.') + 1; 
					String class2 = classname.substring(pos);
					
					/*if (class1.equals("EventMention") || class2.equals("EventMention") )
				    	continue;
					
				TODO	*/
					Label label2 = DynamicLabel.label(class2);
				    
									
					Label label1 = DynamicLabel.label(class1);
					
					if(ignoreList.contains((a1.getCoveredText()).toLowerCase()) || ignoreList.contains((a2.getCoveredText()).toLowerCase()) )
						continue;
					
					    /*   if ((((IdentifiedAnnotation) a1).getSegmentID()).equals("11450-4"))
					    	   if (class1.equals("SignSymptomMention") || class1.equals("DiseaseDisorderMention"))
			                	label1=Label.label("Problem");
					       if ((((IdentifiedAnnotation) a2).getSegmentID()).equals("11450-4"))
					    	   if (class2.equals("SignSymptomMention") ||class2.equals("DiseaseDisorderMention"))
			                	label2=Label.label("Problem");*/
					if (class1.equalsIgnoreCase("Modifier"))
						nodes = _graphDb.findNodes( Mentions.SignSymptomMention, "uid", createID(a1)  );
					else
						nodes = _graphDb.findNodes( label1, "uid", createID(a1)  );

		                	if ( nodes.hasNext() )
		                    { 
		                		a1node=nodes.next();
		                		if(a1node.hasLabel(Mentions.SignSymptomMention) && class2.equalsIgnoreCase("Modifier"))
		                		{	 a1node.removeLabel(Mentions.SignSymptomMention);
		                			a1node.addLabel(label1);
		                		}
		                	
		                    }else {
		                    	a1node=_graphDb.createNode(label1);
			                	a1node.setProperty("originalword", a1.getCoveredText());
			                	insertCommonProperties(a1, a1node);
			                	// insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0),medNode);
			                	docnode.createRelationshipTo(a1node, relations.has_mention);
			                	
			              
		                    }
		                                	
		                 
		                	if (class2.equalsIgnoreCase("Modifier"))
								nodes2 = _graphDb.findNodes( Mentions.SignSymptomMention, "uid", createID(a2)  );
							else
					          nodes2 = _graphDb.findNodes( label2, "uid", createID(a2)  );
		                	                	
		                	if ( nodes2.hasNext() )
		                    { 
		                	   a2node=nodes2.next();
		                	   if(a2node.hasLabel(Mentions.SignSymptomMention) && class2.equalsIgnoreCase("Modifier"))
		                			{
		                		   a1node.removeLabel(Mentions.SignSymptomMention);
		                		   a2node.addLabel(label2);
		                			}
		                    }
		                	else {
		                		a2node=_graphDb.createNode(label2);
			                	a2node.setProperty("originalword", a2.getCoveredText());
			                	insertCommonProperties(a2, a2node);
			                	docnode.createRelationshipTo(a1node, relations.has_mention);
		                	}
		                	
		                	 a1node.createRelationshipTo(a2node, relations.degree_of);	                     
					}
				tx.success();
				}

			LOGGER.info("DegreeOfRelation Transaction Completed Successfully"); //huh
			
			}
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
				    
				    
				    
				    RelationArgument arg2 = t.getArg2();
					Annotation a2 = arg2.getArgument();
					
					classname = a2.getClass().getName();
					pos = classname.lastIndexOf ('.') + 1; 
					String class2 = classname.substring(pos);
					
				/*	if (class1.equals("EventMention") || class2.equals("EventMention") )
				    	continue;
				    	TODO*/
					
					
					Label label2 = DynamicLabel.label(class2);
				    
									
					Label label1 = DynamicLabel.label(class1);
					
					if(ignoreList.contains((a1.getCoveredText()).toLowerCase()) || ignoreList.contains((a2.getCoveredText()).toLowerCase()) )
						continue;
					
					       if ((((IdentifiedAnnotation) a1).getSegmentID()).equals("11450-4"))
					    	   if (class1.equals("SignSymptomMention") || class1.equals("DiseaseDisorderMention"))
			                	label1=Label.label("Problem");
					       if ((((IdentifiedAnnotation) a2).getSegmentID()).equals("11450-4"))
					    	   if (class2.equals("SignSymptomMention") ||class2.equals("DiseaseDisorderMention"))
			                	label2=Label.label("Problem");
					
					 nodes = _graphDb.findNodes( label1, "uid", createID(a1)  );
		                               	
		                	if ( nodes.hasNext() )
		                    { 
		                		a1node=nodes.next();
		                	
		                    }else {
		                    	a1node=_graphDb.createNode(label1);
			                	a1node.setProperty("originalword", a1.getCoveredText());
			                	insertCommonProperties(a1, a1node);
			                	docnode.createRelationshipTo(a1node, relations.has_mention);
		                    }
		                                	
		                 
					
					 
					nodes2 = _graphDb.findNodes( label2, "uid", createID(a2)  );
		                	                	
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
		
		private void insertTimeMentions() {
			
			Collection<TimeMention> tm = JCasUtil.select(jcas,TimeMention.class);
			Collection<TemporalTextRelation> ttr = JCasUtil.select(jcas,TemporalTextRelation.class);
			try (Transaction tx = _graphDb.beginTx()) {
			
				for (Iterator<TimeMention> iterator = tm.iterator(); iterator.hasNext();) {
						TimeMention t = iterator.next();
						t.getTime();
						t.getCoveredText();
						
						Node timeNode = _graphDb.createNode(Mentions.TimeMention);
						timeNode.setProperty("originalword", t.getCoveredText());
						insertCommonProperties(t, timeNode);
				}
			
				tx.success();
		}
				
				try (Transaction tx = _graphDb.beginTx()) {
					for (Iterator<TemporalTextRelation> iterator = ttr.iterator(); iterator.hasNext();) {
							
						TemporalTextRelation t = iterator.next();
						
						Node a1node,a2node;

						RelationArgument arg1 = t.getArg1();
						Annotation a1 = arg1.getArgument();
						
						arg1.getType();
						String classname = a1.getClass().getName();
						int pos = classname.lastIndexOf ('.') + 1; 
						String class1 = classname.substring(pos);
					    
					    
					    
					    RelationArgument arg2 = t.getArg2();
						Annotation a2 = arg2.getArgument();
						
						classname = a2.getClass().getName();
						pos = classname.lastIndexOf ('.') + 1; 
						String class2 = classname.substring(pos);
						

						
						Label label2 = Label.label(class2);
					    
										
						Label label1 = Label.label(class1);
						
									 ResourceIterator<Node> nodes = _graphDb.findNodes( label1, "uid", createID(a1)  );
			                               	
			                	if ( nodes.hasNext() )
			                    { 
			                		a1node=nodes.next();
			                	
			                    }else {
			                    	a1node=_graphDb.createNode(label1);
				                	a1node.setProperty("originalword", a1.getCoveredText());
				                	insertCommonProperties(a1, a1node);
				             //   	docnode.createRelationshipTo(a1node, relations.has_mention);
			                    }
			                                	
			                 
						
						 
						ResourceIterator<Node> nodes2 = _graphDb.findNodes( label2, "uid", createID(a2)  );
			                	                	
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
			                	
			                	 a1node.createRelationshipTo(a2node, relations.temporal_relation);	                     
						}
					tx.success();
					}

				LOGGER.info("temporalTextRelation Transaction Completed Successfully");
				Collection<org.apache.ctakes.typesystem.type.textsem.MedicationMention> mm = JCasUtil.select(jcas,MedicationMention.class);
				for (Iterator<MedicationMention> iterator = mm.iterator(); iterator.hasNext();) {
					
					MedicationMention t = iterator.next();
					Event c = t.getEvent();
				 EventProperties n = c.getProperties();
				 n.getDocTimeRel();
				 n.getContextualModality();
				 c.getId();
				 t.getRelativeTemporalContext();
				 
				 }
					
				
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
		

