package com.canehealth.spring.ctakes.service;


/**
 * @author sanar
 *
 */

import akka.actor.ActorSystem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ctakes.relationextractor.ae.RelationExtractorAnnotator;
import org.apache.ctakes.assertion.medfacts.cleartk.UncertaintyCleartkAnalysisEngine;
import org.apache.ctakes.assertion.util.AssertionConst;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.relationextractor.ae.LocationOfRelationExtractorAnnotator;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation;
import org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention;
import org.apache.ctakes.typesystem.type.textsem.MedicationMention;
import org.apache.ctakes.typesystem.type.textsem.SemanticRoleRelation;
import org.apache.ctakes.typesystem.type.textsem.SignSymptomMention;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.CasIOUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.json.JsonCasSerializer;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.cleartk.util.cr.FilesCollectionReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.canehealth.spring.ctakes.MyClinicalPipelineFactory;
import com.canehealth.spring.ctakes.MyPipeline;
import com.canehealth.spring.ctakes.MyCDAPipeline;
import com.canehealth.spring.ctakes.service.Neo4jPopulation;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@Service
@CacheConfig(cacheNames = "ctakes")
public class CtakesService {
	private static SofaMapping[] sofaMappings = new SofaMapping[31]; // { UIMAFramework.getResourceSpecifierFactory().createSofaMapping(), UIMAFramework.getResourceSpecifierFactory().createSofaMapping() };
	private static Log log = LogFactory.getLog(CtakesService.class);

	public final JCas jcas;
	public final AnalysisEngineDescription aed,red,ted,sed,temp;
    public final boolean is_ccda;
    public boolean CPE;
//	private String pipeline = "FAST";
	@Value("${ctakes.pipeline}")
	private String pipeline = "FAST";
	@Autowired
	private ActorSystem system;  // I am not sure if we need this, but too scared to delete. 


	
	
	
	public CtakesService() throws Exception {
		//jcas = JCasFactory.createJCas();
		
		TypeSystemDescription tsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
		jcas = CasCreationUtils.createCas(tsd, null, null).getJCas();
		is_ccda=false;
		aed = MyPipeline.getAggregateBuilder();
		red = MyPipeline.getRelationBuilder();
		//Sana Edit : Temp
	 	sed= MyPipeline.getSmoking();
		//Sana Edit : 02-jan-2019		
		ted=MyCDAPipeline.getCDAAggregateBuilder();
		temp=MyCDAPipeline.getCda2Pipeline();
				
		/*'Avert your eyes, it may take on other forms!   ~whoosis */
		//sofamaaping();
		//ted.setSofaMappings(sofaMappings);

		// helper not necessarily needed
		/*File desc = new File("C:\\Users\\sanar\\Desktop\\ctakes-spring-boot\\src\\main\\java\\com\\canehealth\\spring\\ctakes\\descriptor.xml");
	    desc.createNewFile();
	    OutputStream out = new FileOutputStream(desc);
		ted.toXML(out,false);*/

		// this is a temporary functionality, once figured out hows rest APi working, this will be moved 
		// alternative for Jcas2json

	    //	getdocument();
		
		
		
	}

	@CacheEvict(allEntries = true)
	public void clearCache() {
	}

	@Cacheable(value = "ctakes")
	public String Jcas2json(String note) throws Exception {
		jcas.reset();
		CPE=false;
		if(CPE) {
		runCollectionProccesingEngine(note, aed);
		return "done";
		}
		
		jcas.setDocumentText(note);
	// CDA Pipeline
		
		
		if (is_ccda) {
			
			SimplePipeline.runPipeline(jcas, ted);
		}
	//	SimplePipeline.runPipeline(jcas, ted);
	
		
   // Plain text pipeline		
			SimplePipeline.runPipeline(jcas, aed);
		
		//SimplePipeline.runPipeline(jcas, sed);
		
		
			CAS cas = jcas.getCas();
			JsonCasSerializer jcs = new JsonCasSerializer();
			jcs.setPrettyPrint(true);
			
			
	    //    jcs.setJsonContext(JsonCasSerializer.JsonContextFormat.omitSubtypes);
	     //   jcs.setJsonContext(JsonCasSerializer.JsonContextFormat.omitContext);
	      //  jcs.setJsonContext(JsonCasSerializer.JsonContextFormat.omitExpandedTypeNames);
			StringWriter sw = new StringWriter();
			jcs.serialize(cas, sw); 
			return jsonClinical(sw.toString(), note);
	}

	@SuppressWarnings("unchecked")
	public String jsonClinical(String ctakes, String note) throws Exception {
		
		
		
//		JCas t=jcas.getView("plaintext");
//		Collection<org.apache.ctakes.typesystem.type.textsem.MedicationMention> mm= JCasUtil.select(t, MedicationMention.class); 
		
		
//		for (Iterator<MedicationMention> iterator = mm.iterator(); iterator.hasNext();) {
//			MedicationMention m = iterator.next();
//			m.getSegmentID();
		
//		}
		
		JSONParser jsonParser = new JSONParser();
		JSONObject obj = new JSONObject();
		try {
			obj = (JSONObject) jsonParser.parse(ctakes);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		obj = (JSONObject) obj.get("_views");
	//	obj = (JSONObject) obj.get("plaintext");
		obj = (JSONObject) obj.get("_InitialView");
		
		
		//alihur edits - obj = (JSONObject) obj.get("_InitialView");
       
	//	JSONArray StrengthAnnotation = (JSONArray) obj.get("StrengthAnnotation");
	//	JSONArray FormAnnotation = (JSONArray) obj.get("FormAnnotation");
	//	JSONArray MeasurementAnnotation = (JSONArray) obj.get("MeasurementAnnotation");
	//	JSONArray DrugChangeStatusAnnotation = (JSONArray) obj.get("DrugChangeStatusAnnotation");
		JSONArray MedicationMention = (JSONArray) obj.get("MedicationMention");
		
		//Sana's edit: Debugging 
//		Collection<org.apache.ctakes.typesystem.type.textsem.MedicationMention> mm= JCasUtil.select(jcas, MedicationMention.class); 
		Collection<org.apache.ctakes.typesystem.type.textsem.SignSymptomMention> ss= JCasUtil.select(jcas, SignSymptomMention.class);
		Collection<org.apache.ctakes.typesystem.type.relation.LocationOfTextRelation>loc_of=JCasUtil.select(jcas, LocationOfTextRelation.class);
		Collection<org.apache.ctakes.typesystem.type.textsem.SemanticRoleRelation> sr= JCasUtil.select(jcas, SemanticRoleRelation.class);
		Collection<org.apache.ctakes.typesystem.type.textsem.DiseaseDisorderMention> dd= JCasUtil.select(jcas, DiseaseDisorderMention.class);
		// --
		
		JSONArray AnatomicalSiteMention = (JSONArray) obj.get("AnatomicalSiteMention");
		JSONArray DiseaseDisorderMention = (JSONArray) obj.get("DiseaseDisorderMention");
		JSONArray SignSymptomMention = (JSONArray) obj.get("SignSymptomMention");
		JSONArray ProcedureMention = (JSONArray) obj.get("ProcedureMention");
		JSONArray WordToken = (JSONArray) obj.get("WordToken");

		JSONObject output = new JSONObject();
	
	//Sana's Edits 
		// this is only needed to show you original text in the following annotations. You can add other annotation types too. 		
		// Note: all these changes are made in 'initial_view' obj in order to show others annotations too
		 
	//	if(StrengthAnnotation!=null)
	//		obj.put("StrengthAnnotation", parseJsonMention(note, WordToken, StrengthAnnotation));
	//	if(FormAnnotation!=null)
	//	obj.put("FormAnnotation", parseJsonMention(note, WordToken, FormAnnotation));
		if(MedicationMention!=null)
			obj.put("MedicationMention", parseJsonMention(note, WordToken, MedicationMention));
//		if(DrugChangeStatusAnnotation!=null)
//			obj.put("DrugChangeStatusAnnotation", parseJsonMention(note, WordToken, DrugChangeStatusAnnotation));
		if(AnatomicalSiteMention!=null)
		obj.put("AnatomicalSiteMention", parseJsonMention(note, WordToken, AnatomicalSiteMention));
		if(DiseaseDisorderMention!=null)
		obj.put("DiseaseDisorderMention", parseJsonMention(note, WordToken, DiseaseDisorderMention));
		if(SignSymptomMention!=null)
		obj.put("SignSymptomMention", parseJsonMention(note, WordToken, SignSymptomMention));
		if(ProcedureMention!=null)
		obj.put("ProcedureMention", parseJsonMention(note, WordToken, ProcedureMention));
		//if(MeasurementAnnotation!=null)
		  //obj.put("MeasurementAnnotation", parseJsonMention(note, WordToken, MeasurementAnnotation));
	
		output.put("Original", obj);
		

	//	Neo4jPopulation2.getInstance()
		//           .insertNodes(jcas);
//		Neo4jCDAPopulation.getInstance()
	//           .insertNodes(jcas.getView("plaintext"));
	//			return output.toJSONString();
		
//		t.getDocumentText();
		// for relation references, CAS_Top object is needed. uncomment this line in that case 
		
		return ctakes;      
	}

	
	//__________________________________________________________________________________________________________________
	//------------------------------------------------------------------------------------------------------------------
	
	private void runCollectionProccesingEngine(String path,AnalysisEngineDescription pipeline) throws UIMAException, IOException {
		
		
		  CollectionReaderDescription collectionReader = FilesCollectionReader.getDescription(path);

				System.out.println("Reading from directory: " + path);
			//	System.out.println("Outputting to directory: " + AssertionConst.evalOutputDir);
				
				/*AnalysisEngineDescription pipelineIncludingUmlsDictionaries = AnalysisEngineFactory.createEngineDescription(
						"desc/analysis_engine/AggregatePlaintextUMLSProcessor");
*/
				 SimplePipeline.runPipeline(collectionReader, pipeline);
			
			    System.out.println("Done at " + new Date());
		
		
	}
	private JSONArray parseJsonMention(String document, JSONArray wordtoken, JSONArray jsonArray) throws Exception {

		JSONArray output = new JSONArray();
	
			for (int i = 0, size = jsonArray.size(); i < size; i++) {
				// this check is needed if you want to add "original_text" attribue for other annotators too
				if(!(jsonArray.get(i) instanceof JSONObject))
				  continue;
				JSONObject objectInArray = (JSONObject) jsonArray.get(i);
		       
				long begin = (long) objectInArray.get("begin");
				long end = (long) objectInArray.get("end");
				String original_word = document.substring((int) begin, (int) end);
				String canonical_form = "";
				for (int i2 = 0, size2 = wordtoken.size(); i2 < size2; i2++) {
					JSONObject tokenInArray = (JSONObject) wordtoken.get(i2);
					long begin2 = (long) tokenInArray.get("begin");
					long end2 = (long) tokenInArray.get("end");
					if (begin == begin2 && end == end2)
						canonical_form = (String) tokenInArray.get("canonicalForm");
				}
				objectInArray.put("originalWord", original_word);
				objectInArray.put("canonicalForm", canonical_form);
				output.add(objectInArray);
			}
		
		
			return output;
	
	}
	
	// utility fnction. array size s hard coded yet
	
	private void sofamaaping() {
		
		sofaMap("org.apache.ctakes.preprocessor.ae.CdaCasInitializer","plaintext",0);

		sofaMap("SimpleSegmentAnnotator","plaintext",30);

		sofaMap("org.apache.ctakes.core.ae.SentenceDetector","plaintext",2);
		sofaMap("org.apache.ctakes.core.ae.TokenizerAnnotatorPTB","plaintext",3);
		sofaMap("org.apache.ctakes.lvg.ae.LvgAnnotator","plaintext",4);
		sofaMap("org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator","plaintext",5);
		sofaMap("org.apache.ctakes.postagger.POSTagger","plaintext",6);
		sofaMap("org.apache.ctakes.chunker.ae.Chunker","plaintext",7);
		sofaMap("org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster","plaintext",8);
		sofaMap("org.apache.ctakes.temporal.eval.Evaluation_ImplBase$CopyNPChunksToLookupWindowAnnotations","plaintext",9);
		sofaMap("org.apache.ctakes.temporal.eval.Evaluation_ImplBase$RemoveEnclosedLookupWindows","plaintext",10);


		//aggregate

		sofaMap("org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator","plaintext",29);
		sofaMap("ClassifiableEntriesAnnotator","plaintext",11);

		sofaMap("DrugMentionAnnotator","plaintext",12);

		sofaMap("org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE","plaintext",13);
		sofaMap("org.apache.ctakes.assertion.medfacts.cleartk.PolarityCleartkAnalysisEngine","plaintext",14);

		sofaMap("org.apache.ctakes.assertion.medfacts.cleartk.UncertaintyCleartkAnalysisEngine","plaintext",15);
		sofaMap("org.apache.ctakes.dependency.parser.ae.ClearNLPSemanticRoleLabelerAE","plaintext",16);

		sofaMap("org.apache.ctakes.constituency.parser.ae.ConstituencyParser","plaintext",17);


		//time
		sofaMap("org.apache.ctakes.temporal.ae.BackwardsTimeAnnotator","plaintext",18);
		sofaMap("org.apache.ctakes.temporal.ae.EventAnnotator","plaintext",19);
		sofaMap("org.apache.ctakes.temporal.pipelines.FullTemporalExtractionPipeline$CopyPropertiesToTemporalEventAnnotator","plaintext",20);
		sofaMap("com.canehealth.spring.ctakes.MyCDAPipeline$AddEvent","plaintext",21);
		sofaMap("com.canehealth.spring.ctakes.service.FilterPrepositionalObjectSites","plaintext",22);
		sofaMap("org.apache.ctakes.temporal.ae.EventTimeSelfRelationAnnotator","plaintext",23);
		sofaMap("org.apache.ctakes.temporal.ae.EventEventRelationAnnotator","plaintext",24);

		sofaMap("aggregate","plaintext",25);

		sofaMap("DegreeOfRelationExtractorAnnotator","plaintext",26);
		sofaMap("LocationOfRelationExtractorAnnotator","plaintext",27);
		sofaMap("ExtractionPrepAnnotator","plaintext",28);
		sofaMap("org.apache.ctakes.template.filler.ae.TemplateFillerAnnotator","plaintext",1);


		Capability[] capabilities = new Capability[] { UIMAFramework.getResourceSpecifierFactory().createCapability() };
		//   capabilities[0].setInputSofas(new String[] { "_InitialView" });
		capabilities[0].setOutputSofas(new String[] { "plaintext" });

		ted.getAnalysisEngineMetaData().setCapabilities(capabilities);
		Capability[] w=ted.getAnalysisEngineMetaData().getCapabilities();
	//	w=temp.getAnalysisEngineMetaData().getCapabilities();

	}
	
	
	private void sofaMap( String componentKey, String view, int index) {
		sofaMappings[index]= UIMAFramework.getResourceSpecifierFactory().createSofaMapping();
		sofaMappings[index].setComponentKey(componentKey);
		sofaMappings[index].setAggregateSofaName(view);
		if (componentKey.equals("org.apache.ctakes.preprocessor.ae.CdaCasInitializer"))
			sofaMappings[index].setComponentSofaName(view);

		//	return sofaMappings;


		
	}
	
	//temporary function
	private String getdocument() throws Exception{
		jcas.reset();
        String note="";
		jcas.setDocumentText(note);

		SimplePipeline.runPipeline(jcas, ted);	
		CAS cas = jcas.getCas();
		JsonCasSerializer jcs = new JsonCasSerializer();
		jcs.setPrettyPrint(true);

		StringWriter sw = new StringWriter();
		jcs.serialize(cas, sw); 
		return jsonClinical(sw.toString(), note);
		
	}
	
	
        
	}
