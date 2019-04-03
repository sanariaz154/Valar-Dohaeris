package com.canehealth.spring.ctakes.service;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.DegreeOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.relation.TemporalTextRelation;
import org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textsem.LabMention;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.ProcedureMention;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.ctakes.typesystem.type.textsem.TimeMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

@SuppressWarnings("deprecation")
public enum Neo4jPopulationDiseaseOriented {
	INSTANCE;

	public static Neo4jPopulationDiseaseOriented getInstance() {
		return INSTANCE;
	}

	static private final Logger LOGGER = Logger.getLogger("Neo4jPopulation2");

	// old-lady static private final String OUTPUT_GRAPH_DB =
	// "C:\\Users\\sanar\\.Neo4jDesktop\\neo4jDatabases\\database-da86d6c8-5091-4ae9-97c2-9c623e210e77\\installation-3.5.0\\data\\databases\\graph.db"/*"C:\\Users\\sanar\\Desktop\\neoctakes\\databases\\graph.db"*/;
	/* a2z-testing *///static private final String OUTPUT_GRAPH_DB = "C:\\Users\\sanar\\.Neo4jDesktop\\neo4jDatabases\\database-d22ae045-5a99-4059-b9be-d9f3e9929e4f\\installation-3.5.0\\data\\databases\\graph.db";
	  static private final String OUTPUT_GRAPH_DB ="C:\\Users\\sanar\\.Neo4jDesktop\\neo4jDatabases\\database-9dec1848-41f6-40f2-9ea8-d1095606608c\\installation-3.5.0\\data\\databases\\graph.db";
	
	
	private GraphDatabaseService _graphDb;
	public Node diseasenode, docnode;
	private JCas jcas;
	private ResourceIterator<Node> nodes, nodes2;
	boolean isFreeText = true;
	List<String> ignoreList = Arrays.asList("history", "condition", /* "anterior", */"medications", "medication",
			"problem", "disease", "diseases", "disorder", "sign",
			/* "oral", "oral tablet", */ "pharmaceutical preparations", "test", "tests", "family history", "program",
			"programs", "syndrome");

	public enum Mentions implements Label {
		document, MedicationMention, SignSymptomMention, DiseaseDisorderMention, AnatomicalSiteMention,
		ProcedureMention, Modifier, TimeMention, MedicationFrequency, MedicationDosage, MedicationStrength,
		MedicationAllergy, /* UMLSConcept */EventProperties, MedicationForm, MedicationRoute, Cause,LabMention; // sorry!!!!!

	} // may be i can beautify it a little . if the code works then it deserves

	public enum relations implements RelationshipType {
		// utility
		has_mention, /* has_umlsontologyconcept, */has_event_properties, is_a, location_of, degree_of,
		// related to Medication Mention
		has_frequency, has_form, has_Dosage, has_strength, has_allergy, has_route,

		// related to DiseaseDisorder
		has_alleviatingFactor, has_associatedSignSymptom
		// related to SignSymptom
		, has_bodyLocation
		// related to AnatomicalSite
		, has_bodyLaterality, has_bodySide, has_bodysite
		// related to Procedure

		// related to time
		, temporal_relation, has_relativeTemporalContext, associated_with_disease, has_symptoms, has_cause, has_treatment_proceedure,has_treatment_lab, has_treatment_medication
	}

	String doc_id;

	public int insertNodes(JCas j) throws CASException {
		this.jcas = j;
		connectToGraph(OUTPUT_GRAPH_DB);

		// jcas.getViewIterator();

		// insertTimeMentions();

	
		insertNodeDiseaseDisorder();
		insertNodeSignSymptoms();
		insertNodeMedication();
	    insertNodeProcedure();
		insertNodeAnatomicalSite();
		insertDegreeOfTextRelations();
		insertLocationOfTextRelations();

		_graphDb.shutdown();
		LOGGER.info("Registered Shutdown hook");
		return 1;
	}

	private String createID(Object o) {

		return String.valueOf(((IdentifiedAnnotation) o).getBegin()) + "-"
				+ String.valueOf(((IdentifiedAnnotation) o).getEnd()) + "-" + doc_id; // + String.valueOf(doc_id);
		// return A+B+String.valueOf(doc_id);
	}

	private String getCUI(Object o) {

		return ((UmlsConcept) ((IdentifiedAnnotation) o).getOntologyConceptArr(0)).getCui();
	}

	private Relationship isRelated(Node a, Node b, RelationshipType type) {
		for (Relationship r : a.getRelationships(type)) {
			if (r.getOtherNode(a).equals(b))
				return r;
		}
		return null;
	}

	private void insertCommonProperties(Object o, Node node) {
		node.setProperty("uid", createID(o));
		node.setProperty("hash", o.hashCode());
		// node.setProperty("begin", ((IdentifiedAnnotation) o).getBegin()); //we dont
		// need begin and end- if needed, can be get through uid of node
		// node.setProperty("end", ((IdentifiedAnnotation) o).getEnd());
		// TODO node.setProperty("discoverytechnique", );
		node.setProperty("confidence", ((IdentifiedAnnotation) o).getConfidence());
		// node.setProperty("event", ((EventMention) o).getEvent()); TODO, get
		// confidence of event, doctime etc . get confidence in yourself too
		node.setProperty("generic", ((IdentifiedAnnotation) o).getGeneric());
		node.setProperty("historyof", ((IdentifiedAnnotation) o).getHistoryOf());

		// node.setProperty("originalword",((IdentifiedAnnotation) o).getCoveredText());
		node.setProperty("polarity", ((IdentifiedAnnotation) o).getPolarity());
		node.setProperty("uncertainty", ((IdentifiedAnnotation) o).getUncertainty()); // A girl is very uncertain
		node.setProperty("conditional", ((IdentifiedAnnotation) o).getConditional());

		node.setProperty("subject", Optional.ofNullable(((IdentifiedAnnotation) o).getSubject()).orElse("N/A"));
		// A girl
		// doen'nt
		// know
		// how
		// to
		// do
		// this
		// .
		// yet

	}

	private void insertEventProperties(Event e, Node node) {

		// try/catch???
		// node.setProperty("uncertainty",e.getUncertainty()); // A girl is very
		// uncertain
		// node.setProperty("conditional",e.getConditional() );
		Node evnode = _graphDb.createNode(Mentions.EventProperties);
		evnode.setProperty("event-subject", Optional.ofNullable(e.getSubject()).orElse("N/A"));
		evnode.setProperty("event-hashcode", e.hashCode());
		evnode.setProperty("event-polarity", e.getPolarity());
		evnode.setProperty("event-confidence", e.getConfidence());
		evnode.setProperty("event-docTimeRel", Optional.ofNullable(e.getProperties().getDocTimeRel()).orElse("N/A"));
		evnode.setProperty("event-catogary", Optional.ofNullable(e.getProperties().getCategory()).orElse("N/A"));
		evnode.setProperty("event-aspect", Optional.ofNullable(e.getProperties().getAspect()).orElse("N/A"));
		evnode.setProperty("event-permanence", Optional.ofNullable(e.getProperties().getAspect()).orElse("N/A"));
		evnode.setProperty("event-contextualAspect",
				Optional.ofNullable(e.getProperties().getContextualAspect()).orElse("N/A"));
		evnode.setProperty("event-contextualModality",
				Optional.ofNullable(e.getProperties().getContextualModality()).orElse("N/A"));
		evnode.setProperty("event-degree", Optional.ofNullable(e.getProperties().getDegree()).orElse("N/A"));
		node.createRelationshipTo(evnode, relations.has_event_properties);
	}

	private void insertOntologyConcenpt(UmlsConcept umls_ont, Node node) { // ABANDONED TO THE MISTS OF TIME!!!

		node.setProperty("code", umls_ont.getCode());
		node.setProperty("preferredtext", umls_ont.getPreferredText());
		node.setProperty("score", umls_ont.getScore());
		node.setProperty("disambiguated", umls_ont.getDisambiguated());
		node.setProperty("codingScheme", Optional.ofNullable(umls_ont.getCodingScheme()).orElse("N/A"));
		node.setProperty("tui", umls_ont.getTui());
		node.setProperty("cui", umls_ont.getCui());
		node.setProperty("oid", umls_ont.getOid());

	}

	private void insertNodeMedication() {

		Collection<org.apache.ctakes.typesystem.type.textsem.MedicationMention> mm = JCasUtil.select(jcas,
				MedicationMention.class);
		
		
		
		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<MedicationMention> iterator = mm.iterator(); iterator.hasNext();) {
				MedicationMention t = iterator.next();
				t.getSegmentID();
				t.getCoveredText();
				if(t.getOntologyConceptArr()==null) 
					continue;
					
				
			
					if (ignoreList.contains((t.getCoveredText()).toLowerCase())
							|| !((t.getSegmentID()).equals("7")))
						continue;
				
     				nodes = _graphDb.findNodes(Mentions.MedicationMention, "cui", getCUI(t));
					Relationship medrel;

					if (nodes.hasNext()) {
						Node medNode = nodes.next();
						medrel = isRelated(diseasenode, medNode, relations.has_treatment_medication);
						if (medrel != null) {
							int w = (int) medrel.getProperty("weightage");
							medrel.setProperty("weightage", w + 1);
						} else {

							medrel = diseasenode.createRelationshipTo(medNode, relations.has_treatment_medication);
							medrel.setProperty("weightage", 1);
						}

					}
					else {						
						Node medNode = _graphDb.createNode(Mentions.MedicationMention);
						medNode.setProperty("originalword", t.getCoveredText());
						insertCommonProperties(t, medNode);
						
						
							 t.getOntologyConceptArr(0);
							if( t.getOntologyConceptArr(0).getCode()!=null)
								insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0), medNode);
						insertEventProperties(t.getEvent(), medNode);

						
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

						
						
						medrel = diseasenode.createRelationshipTo(medNode, relations.has_treatment_medication);
						medrel.setProperty("weightage", 1);
					}	
					
			

			}
			tx.success();
		}
		LOGGER.info("Medication Transaction Completed Successfully");
	}

	private void insertNodeSignSymptoms() {
		Label lb = Mentions.SignSymptomMention;

		Collection<Segment> s = JCasUtil.select(jcas, Segment.class);
		Collection<org.apache.ctakes.typesystem.type.textsem.SignSymptomMention> ss = JCasUtil.select(jcas,
				SignSymptomMention.class);
		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<SignSymptomMention> iterator = ss.iterator(); iterator.hasNext();) {

				SignSymptomMention t = iterator.next();
				t.getCoveredText();
				t.getSegmentID();

				if (ignoreList.contains((t.getCoveredText()).toLowerCase()))
					continue;

				if ((t.getSegmentID()).equals("3")) {
					nodes = _graphDb.findNodes(Mentions.SignSymptomMention, "cui", getCUI(t));
					Relationship symrel;

					if (nodes.hasNext()) {
						Node symNode = nodes.next();
						symrel = isRelated(diseasenode, symNode, relations.has_symptoms);
						if (symrel != null) {
							int w = (int) symrel.getProperty("weightage");
							symrel.setProperty("weightage", w + 1);
						} else {

							symrel = diseasenode.createRelationshipTo(symNode, relations.has_symptoms);
							symrel.setProperty("weightage", 1);
						}

					} else {

						// lb = Mentions.SignSymptomMention;
						Node symNode = _graphDb.createNode(lb);
						symNode.setProperty("originalword", t.getCoveredText());
						insertCommonProperties(t, symNode);
						insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0), symNode);
						insertEventProperties(t.getEvent(), symNode);

						symrel = diseasenode.createRelationshipTo(symNode, relations.has_symptoms);
						symrel.setProperty("weightage", 1);
					}

				}
				if ((t.getSegmentID()).equals("4")) {
					lb = Mentions.Cause;
					nodes = _graphDb.findNodes(Mentions.Cause, "cui", getCUI(t));
					Relationship symrel;

					if (nodes.hasNext()) {
						Node symNode = nodes.next();
						symrel = isRelated(diseasenode, symNode, relations.has_cause);
						if (symrel != null) {
							int w = (int) symrel.getProperty("weightage");
							symrel.setProperty("weightage", w + 1);
						} else {

							symrel = diseasenode.createRelationshipTo(symNode, relations.has_cause);
							symrel.setProperty("weightage", 1);
						}

					} else {

						// lb = Mentions.SignSymptomMention;
						Node symNode = _graphDb.createNode(lb);
						symNode.setProperty("originalword", t.getCoveredText());
						insertCommonProperties(t, symNode);
						insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0), symNode);
						insertEventProperties(t.getEvent(), symNode);

						symrel = diseasenode.createRelationshipTo(symNode, relations.has_cause);
						symrel.setProperty("weightage", 1);
					}

				}

			}

			tx.success();
		}

		LOGGER.info("Sign/Symptoms Transaction Completed Successfully");
	}

	private void insertNodeAnatomicalSite() {
		// connectToGraph(OUTPUT_GRAPH_DB);

		Collection<org.apache.ctakes.typesystem.type.textsem.AnatomicalSiteMention> as = JCasUtil.select(jcas,
				AnatomicalSiteMention.class);
		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<AnatomicalSiteMention> iterator = as.iterator(); iterator.hasNext();) {

				AnatomicalSiteMention t = iterator.next();
				if (ignoreList.contains((t.getCoveredText()).toLowerCase()))
					continue;
				/// Node siteNode = _graphDb.createNode(Mentions.AnatomicalSiteMention);

				if ((t.getSegmentID()).equals("3") || (t.getSegmentID()).equals("2")) {
					Label lb = Mentions.AnatomicalSiteMention;
					nodes = _graphDb.findNodes(Mentions.AnatomicalSiteMention, "cui", getCUI(t));
					Relationship arel;

					if (nodes.hasNext()) {
						Node aNode = nodes.next();
						arel = isRelated(diseasenode, aNode, relations.has_bodysite);
						if (arel != null) {
							int w = (int) arel.getProperty("weightage");
							arel.setProperty("weightage", w + 1);
						} else {

							arel = diseasenode.createRelationshipTo(aNode, relations.has_bodysite);
							arel.setProperty("weightage", 1);
						}

					} else {

						// lb = Mentions.SignSymptomMention;
						Node aNode = _graphDb.createNode(lb);
						aNode.setProperty("originalword", t.getCoveredText());
						insertCommonProperties(t, aNode);
						insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0), aNode);

						arel = diseasenode.createRelationshipTo(aNode, relations.has_bodysite);
						arel.setProperty("weightage", 1);
					}

				}
			}

			tx.success();
		}

		LOGGER.info("Anatomical Site Transaction Completed Successfully");
	}

	private void insertNodeProcedure() {
		
		Collection<org.apache.ctakes.typesystem.type.textsem.ProcedureMention> p = JCasUtil.select(jcas,
				ProcedureMention.class);
		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<ProcedureMention> iterator = p.iterator(); iterator.hasNext();) {

				ProcedureMention t = iterator.next();

			
			
				Label lb=Mentions.ProcedureMention;
			
				if ((t.getSegmentID()).equals("7")) {   //treatment
				
					nodes = _graphDb.findNodes(Mentions.ProcedureMention, "cui", getCUI(t));
					Relationship prorel;

					if (nodes.hasNext()) {
						Node procNode = nodes.next();
						prorel = isRelated(diseasenode, procNode, relations.has_treatment_proceedure);
						if (prorel != null) {
							int w = (int) prorel.getProperty("weightage");
							prorel.setProperty("weightage", w + 1);
						} else {

							prorel = diseasenode.createRelationshipTo(procNode, relations.has_treatment_proceedure);
							prorel.setProperty("weightage", 1);
						}

					}
					else {

						// lb = Mentions.SignSymptomMention;
						Node procNode = _graphDb.createNode(lb);
						procNode.setProperty("originalword", t.getCoveredText());
						insertCommonProperties(t, procNode);
						insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0), procNode);
						insertEventProperties(t.getEvent(), procNode);

						prorel = diseasenode.createRelationshipTo(procNode, relations.has_treatment_proceedure);
						prorel.setProperty("weightage", 1);
					}	
					
				}
			}

			tx.success();
		}

		LOGGER.info("Procedure Transaction Completed Successfully");
		
		
		Collection<LabMention> lp = JCasUtil.select(jcas,LabMention.class);
		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<LabMention> iterator = lp.iterator(); iterator.hasNext();) {

				LabMention t = iterator.next();

			
			
				Label lb=Mentions.LabMention;
			
				if ((t.getSegmentID()).equals("7")) {   //treatment
				
					nodes = _graphDb.findNodes(lb, "cui", getCUI(t));
					Relationship prorel;

					if (nodes.hasNext()) {
						Node procNode = nodes.next();
						prorel = isRelated(diseasenode, procNode, relations.has_treatment_lab);
						if (prorel != null) {
							int w = (int) prorel.getProperty("weightage");
							prorel.setProperty("weightage", w + 1);
						} else {

							prorel = diseasenode.createRelationshipTo(procNode, relations.has_treatment_lab);
							prorel.setProperty("weightage", 1);
						}

					}
					else {

						
						Node procNode = _graphDb.createNode(lb);
						procNode.setProperty("originalword", t.getCoveredText());
						insertCommonProperties(t, procNode);
						insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0), procNode);
						insertEventProperties(t.getEvent(), procNode);

						prorel = diseasenode.createRelationshipTo(procNode, relations.has_treatment_lab);
						prorel.setProperty("weightage", 1);
					}	
					
				}
			}

			tx.success();
			LOGGER.info("Lab Transaction Completed Successfully");
		}
		
	}

	private void insertNodeDiseaseDisorder() {
		// connectToGraph(OUTPUT_GRAPH_DB);

		Label lb = Mentions.DiseaseDisorderMention;
		Collection<org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention> dd = JCasUtil.select(jcas,
				DiseaseDisorderMention.class);

		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<DiseaseDisorderMention> iterator = dd.iterator(); iterator.hasNext();) {

				DiseaseDisorderMention t = iterator.next();
				t.getCoveredText();
				t.getSegmentID();

				if (ignoreList.contains((t.getCoveredText()).toLowerCase()))
					continue;

				nodes = _graphDb.findNodes(Mentions.DiseaseDisorderMention, "cui", getCUI(t));

				if ((t.getSegmentID()).equals("1")) { // TITLE

					if (nodes.hasNext())
						diseasenode = nodes.next();
					else {
						diseasenode = _graphDb.createNode(lb);
						diseasenode.setProperty("originalword", t.getCoveredText());
						insertCommonProperties(t, diseasenode);
						insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0), diseasenode);
						insertEventProperties(t.getEvent(), diseasenode);
					}
				}

				if ((t.getSegmentID()).equals("4")) {   //causes
					lb = Mentions.Cause;
					nodes = _graphDb.findNodes(Mentions.Cause, "cui", getCUI(t));
					Relationship symrel;

					if (nodes.hasNext()) {
						Node symNode = nodes.next();
						symrel = isRelated(diseasenode, symNode, relations.has_cause);
						if (symrel != null) {
							int w = (int) symrel.getProperty("weightage");
							symrel.setProperty("weightage", w + 1);
						} else {

							symrel = diseasenode.createRelationshipTo(symNode, relations.has_cause);
							symrel.setProperty("weightage", 1);
						}

					} else {

						// lb = Mentions.SignSymptomMention;
						Node symNode = _graphDb.createNode(lb);
						symNode.setProperty("originalword", t.getCoveredText());
						insertCommonProperties(t, symNode);
						insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0), symNode);
						insertEventProperties(t.getEvent(), symNode);

						symrel = diseasenode.createRelationshipTo(symNode, relations.has_cause);
						symrel.setProperty("weightage", 1);
					}

				}

				else {
					Relationship disrel;
					Node disnode;
					if (nodes.hasNext()) {
						disnode = nodes.next();
						if (!((disnode.getProperty("cui")).equals((diseasenode).getProperty("cui")))) {
							disrel = isRelated(disnode, diseasenode, relations.associated_with_disease);
							if (disrel != null) {
								int w = (int) disrel.getProperty("weightage");
								disrel.setProperty("weightage", w + 1);
							} else {

								disrel = diseasenode.createRelationshipTo(disnode, relations.associated_with_disease);
								disrel.setProperty("weightage", 1);
							}
						}
					}

					else {
						disnode = _graphDb.createNode(lb);
						disnode.setProperty("originalword", t.getCoveredText());
						insertCommonProperties(t, disnode);
						insertOntologyConcenpt((UmlsConcept) t.getOntologyConceptArr(0), disnode);
						insertEventProperties(t.getEvent(), disnode);
						disrel = diseasenode.createRelationshipTo(disnode, relations.associated_with_disease);
						disrel.setProperty("weightage", 1);
					}

				}

			}

			tx.success();
		}

		LOGGER.info("DiseaseDisorder Transaction Completed Successfully");
	}

	// TODO //PTP: if event mentions are note suppose to record in graph, why
	// creating new nodes here??
	private void insertDegreeOfTextRelations() {
		Collection<DegreeOfTextRelation> deg_of = JCasUtil.select(jcas, DegreeOfTextRelation.class);
		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<DegreeOfTextRelation> iterator = deg_of.iterator(); iterator.hasNext();) {

				DegreeOfTextRelation t = iterator.next();

				Node a1node, a2node;

				RelationArgument arg1 = t.getArg1();
				Annotation a1 = arg1.getArgument();

				arg1.getType();
				String classname = a1.getClass().getName();
				int pos = classname.lastIndexOf('.') + 1;
				String class1 = classname.substring(pos);

				RelationArgument arg2 = t.getArg2();
				Annotation a2 = arg2.getArgument();

				if(((IdentifiedAnnotation) a1).getOntologyConceptArr()==null || ((IdentifiedAnnotation) a2).getOntologyConceptArr()== null) 
					continue;
				
				classname = a2.getClass().getName();
				pos = classname.lastIndexOf('.') + 1;
				String class2 = classname.substring(pos);

				/*
				 * if (class1.equals("EventMention") || class2.equals("EventMention") )
				 * continue;
				 * 
				 * TODO
				 */
				Label label2 = DynamicLabel.label(class2);

				Label label1 = DynamicLabel.label(class1);

				if (ignoreList.contains((a1.getCoveredText()).toLowerCase())
						|| ignoreList.contains((a2.getCoveredText()).toLowerCase()))
					continue;

				/*
				 * if ((((IdentifiedAnnotation) a1).getSegmentID()).equals("11450-4")) if
				 * (class1.equals("SignSymptomMention") ||
				 * class1.equals("DiseaseDisorderMention")) label1=Label.label("Problem"); if
				 * ((((IdentifiedAnnotation) a2).getSegmentID()).equals("11450-4")) if
				 * (class2.equals("SignSymptomMention")
				 * ||class2.equals("DiseaseDisorderMention")) label2=Label.label("Problem");
				 */
				if (class1.equalsIgnoreCase("Modifier"))
					nodes = _graphDb.findNodes(Mentions.SignSymptomMention, "uid",getCUI(a1));
				else
					nodes = _graphDb.findNodes(label1, "uid", getCUI(a1));

				if (nodes.hasNext()) {
					a1node = nodes.next();
					if (a1node.hasLabel(Mentions.SignSymptomMention) && class2.equalsIgnoreCase("Modifier")) {
						a1node.removeLabel(Mentions.SignSymptomMention);
						a1node.addLabel(label1);
					}

				} else {
					a1node = _graphDb.createNode(label1);
					a1node.setProperty("originalword", a1.getCoveredText());
					insertCommonProperties(a1, a1node);
					insertOntologyConcenpt((UmlsConcept) ((IdentifiedAnnotation) a1).getOntologyConceptArr(0),a1node);
					insertEventProperties(((EventMention) a1).getEvent(), a1node);
					diseasenode.createRelationshipTo(a1node, relations.has_symptoms);

				}

				if (class2.equalsIgnoreCase("Modifier"))
					nodes2 = _graphDb.findNodes(Mentions.SignSymptomMention, "uid", getCUI(a2));
				else
					nodes2 = _graphDb.findNodes(label2, "uid", getCUI(a2));

				if (nodes2.hasNext()) {
					a2node = nodes2.next();
					if (a2node.hasLabel(Mentions.SignSymptomMention) && class2.equalsIgnoreCase("Modifier")) {
						a1node.removeLabel(Mentions.SignSymptomMention);
						a2node.addLabel(label2);
					}
				} else {
					a2node = _graphDb.createNode(label2);
					a2node.setProperty("originalword", a2.getCoveredText());
					insertCommonProperties(a2, a2node);
					insertOntologyConcenpt((UmlsConcept) ((IdentifiedAnnotation) a2).getOntologyConceptArr(0),a2node);
					insertEventProperties(((EventMention) a2).getEvent(), a2node);
					diseasenode.createRelationshipTo(a1node, relations.has_symptoms);
				}

				a1node.createRelationshipTo(a2node, relations.degree_of);
			}
			tx.success();
		}

		LOGGER.info("DegreeOfRelation Transaction Completed Successfully"); // huh

	}

	private void insertLocationOfTextRelations() {
		Collection<org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation> loc_of = JCasUtil.select(jcas,
				LocationOfTextRelation.class);

		try (Transaction tx = _graphDb.beginTx()) {
			for (Iterator<LocationOfTextRelation> iterator = loc_of.iterator(); iterator.hasNext();) {

				LocationOfTextRelation t = iterator.next();

				Node a1node, a2node;

				RelationArgument arg1 = t.getArg1();
				Annotation a1 = arg1.getArgument();
				if (!((((IdentifiedAnnotation) a1).getSegmentID()).equals("2")
						|| (((IdentifiedAnnotation) a1).getSegmentID()).equals("3")))
					continue;	
				arg1.getType();
				String classname = a1.getClass().getName();
				int pos = classname.lastIndexOf('.') + 1;
				String class1 = classname.substring(pos);

				RelationArgument arg2 = t.getArg2();
				Annotation a2 = arg2.getArgument();

				classname = a2.getClass().getName();
				pos = classname.lastIndexOf('.') + 1;
				String class2 = classname.substring(pos);
				relations rel;
				Relationship disrel;
				if (class1.equals("EventMention") || class2.equals("EventMention"))
					continue;

				if (class1.equals("DiseaseDisorderMention")) {
					rel = relations.associated_with_disease;

				} else if (class1.equals("SignSymptomMention")) {
					rel = relations.has_symptoms;
				} else {

					rel = relations.has_bodysite;
				}

				Label label2 = DynamicLabel.label(class2);

				Label label1 = DynamicLabel.label(class1);

				if (ignoreList.contains((a1.getCoveredText()).toLowerCase())
						|| ignoreList.contains((a2.getCoveredText()).toLowerCase()))
					continue;

				nodes = _graphDb.findNodes(label1, "cui", getCUI(a1));

				if (nodes.hasNext()) {
					a1node = nodes.next();

					if (!((a1node.getProperty("cui")).equals((diseasenode).getProperty("cui")))) {

						disrel = isRelated(a1node, diseasenode, rel);

						if (disrel != null) {
							int w = (int) disrel.getProperty("weightage");
							disrel.setProperty("weightage", w + 1);
						} else {

							disrel = diseasenode.createRelationshipTo(a1node, rel);
							disrel.setProperty("weightage", 1);
						}
					}

				} else {

					a1node = _graphDb.createNode(label1);
					a1node.setProperty("originalword", a1.getCoveredText());
					insertCommonProperties(a1, a1node);
					insertOntologyConcenpt((UmlsConcept) ((IdentifiedAnnotation) a1).getOntologyConceptArr(0), a1node);
					if (!(class1.equals("AnatomicalSiteMention"))) {
						insertEventProperties(((EventMention) a1).getEvent(), a1node);
					}
					disrel = diseasenode.createRelationshipTo(a1node, rel);
					disrel.setProperty("weightage", 1);
				}

				nodes2 = _graphDb.findNodes(label2, "uid", getCUI(a2));

				if (nodes2.hasNext()) {
					a2node = nodes2.next();
					if (!((a2node.getProperty("cui")).equals((diseasenode).getProperty("cui")))) {

						disrel = isRelated(a2node, diseasenode, rel);

						if (disrel != null) {
							int w = (int) disrel.getProperty("weightage");
							disrel.setProperty("weightage", w + 1);
						} else {

							disrel = diseasenode.createRelationshipTo(a2node, rel);
							disrel.setProperty("weightage", 1);
						}
					}

				} else {

					a2node = _graphDb.createNode(label2);
					a2node.setProperty("originalword", a2.getCoveredText());
					insertCommonProperties(a2, a2node);
					insertOntologyConcenpt((UmlsConcept) ((IdentifiedAnnotation) a2).getOntologyConceptArr(0), a2node);
					if (!(class2.equals("AnatomicalSiteMention"))) {
						insertEventProperties(((EventMention) a2).getEvent(), a2node);
					}
					disrel = diseasenode.createRelationshipTo(a2node, rel);
					disrel.setProperty("weightage", 1);
				}
				a1node.createRelationshipTo(a2node, relations.location_of);
			}
			tx.success();
		}

		LOGGER.info("locationOftextRelation Transaction Completed Successfully"); // huh

	}

	private void insertTimeMentions() {

		Collection<TimeMention> tm = JCasUtil.select(jcas, TimeMention.class);
		Collection<TemporalTextRelation> ttr = JCasUtil.select(jcas, TemporalTextRelation.class);
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

				Node a1node, a2node;

				RelationArgument arg1 = t.getArg1();
				Annotation a1 = arg1.getArgument();

				arg1.getType();
				String classname = a1.getClass().getName();
				int pos = classname.lastIndexOf('.') + 1;
				String class1 = classname.substring(pos);

				RelationArgument arg2 = t.getArg2();
				Annotation a2 = arg2.getArgument();

				classname = a2.getClass().getName();
				pos = classname.lastIndexOf('.') + 1;
				String class2 = classname.substring(pos);

				Label label2 = Label.label(class2);

				Label label1 = Label.label(class1);

				ResourceIterator<Node> nodes = _graphDb.findNodes(label1, "uid", createID(a1));

				if (nodes.hasNext()) {
					a1node = nodes.next();

				} else {
					a1node = _graphDb.createNode(label1);
					a1node.setProperty("originalword", a1.getCoveredText());
					insertCommonProperties(a1, a1node);
					// docnode.createRelationshipTo(a1node, relations.has_mention);
				}

				ResourceIterator<Node> nodes2 = _graphDb.findNodes(label2, "uid", createID(a2));

				if (nodes2.hasNext()) {
					a2node = nodes2.next();

				} else {
					a2node = _graphDb.createNode(label2);
					a2node.setProperty("originalword", a2.getCoveredText());
					insertCommonProperties(a2, a2node);
					docnode.createRelationshipTo(a1node, relations.has_mention);
				}

				a1node.createRelationshipTo(a2node, relations.temporal_relation);
			}
			tx.success();
		}

		LOGGER.info("temporalTextRelation Transaction Completed Successfully");
		Collection<org.apache.ctakes.typesystem.type.textsem.MedicationMention> mm = JCasUtil.select(jcas,
				MedicationMention.class);
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

	private void createOutputGraph(final String outputDbPath) {
		final File outputGraph = new File(outputDbPath);
		final String path = outputGraph.getAbsolutePath();
		if (!outputGraph.exists()) {
			LOGGER.info("Creating output Graph " + path);
			outputGraph.mkdirs();

		} else {

		//	LOGGER.info("Replacing existing output Graph " + path);
		//	deleteDirectory(outputGraph);
			LOGGER.info("Modifying existing output Graph " + path);
			
		}

	}

	static private boolean deleteDirectory(final File directory) {
		final File[] files = directory.listFiles();
		if (files == null) {
			return true;
		}
		boolean ok = true;
		for (File file : files) {
			if (file.isDirectory()) {
				ok &= deleteDirectory(file);
			}
			ok &= file.delete();
		}
		return ok;
	}

	public String getOutputGraphDb() {
		return OUTPUT_GRAPH_DB;
	}

	public GraphDatabaseService connectToGraph() {

		return connectToGraph(getOutputGraphDb());
	}

	public GraphDatabaseService connectToGraph(final String graphDbPath) {
		if (_graphDb != null) {
			return _graphDb;
		}
		createOutputGraph(graphDbPath);
		final File graphDbFile = new File(graphDbPath);
		if (!graphDbFile.isDirectory()) {
			LOGGER.error("No Database exists at: " + graphDbPath);
			System.exit(-1);
		}
		_graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(graphDbFile);
		if (!_graphDb.isAvailable(500)) {
			LOGGER.error("Could not initialize neo4j connection for: " + graphDbPath);
			System.exit(-1);
		}
	//	registerShutdownHook(_graphDb);
		return _graphDb;
	}

	public GraphDatabaseService getGraph() {
		if (_graphDb == null) {
			return connectToGraph();
		}
		return _graphDb;
	}

	static private void registerShutdownHook(final GraphDatabaseService graphDb) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		/*
		 * Runtime.getRuntime().addShutdownHook( new Thread( () -> { try {
		 * graphDb.shutdown(); } catch ( LifecycleException | RotationTimeoutException
		 * multE ) { // ignore } } ) );
		 */

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}

}

// If you're reading this, then my program is probably a success
// Hahah just kidding :D
