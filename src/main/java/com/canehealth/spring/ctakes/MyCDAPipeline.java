package com.canehealth.spring.ctakes;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation;
import org.apache.ctakes.assertion.medfacts.cleartk.PolarityCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.UncertaintyCleartkAnalysisEngine;
import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.relationextractor.pipelines.*;
import org.apache.ctakes.template.filler.ae.TemplateFillerAnnotator;
import org.apache.ctakes.relationextractor.ae.*;
import org.apache.ctakes.clinicalpipeline.ClinicalPipelineFactory;
import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.CDASegmentAnnotator;
import org.apache.ctakes.core.ae.RegexSectionizer;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.resource.FileResourceImpl;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.dependency.parser.ae.ClearNLPSemanticRoleLabelerAE;
import org.apache.ctakes.dictionary.lookup.ae.UmlsDictionaryLookupAnnotator;
import org.apache.ctakes.dictionary.lookup2.ae.AbstractJCasTermAnnotator;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.dictionary.lookup2.ae.JCasTermAnnotator;
import org.apache.ctakes.drugner.DrugMention;
import org.apache.ctakes.drugner.DrugModel;
import org.apache.ctakes.smokingstatus.ae.*;
import org.apache.ctakes.drugner.ae.DrugMentionAnnotator;
import com.canehealth.spring.ctakes.service.myDrugMentionAnnotator;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.temporal.ae.BackwardsTimeAnnotator;
import org.apache.ctakes.temporal.ae.DocTimeRelAnnotator;
import org.apache.ctakes.temporal.ae.EventAnnotator;
import org.apache.ctakes.temporal.ae.EventEventRelationAnnotator;
import org.apache.ctakes.temporal.ae.EventTimeRelationAnnotator;
import org.apache.ctakes.temporal.ae.EventTimeSelfRelationAnnotator;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.CopyNPChunksToLookupWindowAnnotations;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase.RemoveEnclosedLookupWindows;
import org.apache.ctakes.temporal.pipelines.FullTemporalExtractionPipeline.CopyPropertiesToTemporalEventAnnotator;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.SofaMappingFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.analysis_engine.metadata.SofaMapping; 

import com.google.common.collect.Lists;
import com.canehealth.spring.ctakes.service.CdaCasInitializer;
import com.canehealth.spring.ctakes.service.FilterPrepositionalObjectSites;
import com.canehealth.spring.ctakes.service.Sectionizer;
//import org.apache.ctakes.typesystem.type.textspan.SectionHeading;
import com.canehealth.spring.ctakes.service.myCDASegmentAnnotator;
//import org.apache.ctakes.typesystem.type.textspan.SectionHeading;


public class MyCDAPipeline {	

	private static SofaMapping[] sofaMappings = new SofaMapping[13]; 
	private static SofaMapping[] sofaMap = new SofaMapping[] {UIMAFramework.getResourceSpecifierFactory().createSofaMapping()};
	 
	/////////////////////////////////////////////////////////////////////////////////////////////
	public static AnalysisEngineDescription getCDAAggregateBuilder() throws Exception {
		AggregateBuilder builder = new AggregateBuilder();
		TypeSystemDescription typeSystem = TypeSystemDescriptionFactory
 				.createTypeSystemDescription();
		AnalysisEngineDescription cdaseg=AnalysisEngineFactory.createEngineDescription( myCDASegmentAnnotator.class,typeSystem );
		builder.add(cdaseg);
		AnalysisEngineDescription cdacas=AnalysisEngineFactory.createEngineDescription( CdaCasInitializer.class );
		builder.add(cdacas);
		
	
		
	 	
		/******************/
		
		
		//sofaMap[0].setComponentKey("SimpleSegmentAnnotator");
	//	AnalysisEngineDescription simplesegment = AnalysisEngineFactory.createEngineDescription(SimpleSegmentAnnotator.class);
	//	simplesegment.setSofaMappings(sofaMap);
//		builder.add(simplesegment);
		/******************/
		
		
		AnalysisEngineDescription sentencedetector = SentenceDetector.createAnnotatorDescription();
		builder.add(sentencedetector);
		/******************/
		
		
		AnalysisEngineDescription tokenizer = TokenizerAnnotatorPTB.createAnnotatorDescription();
		builder.add(tokenizer);
		/******************/
		
		AnalysisEngineDescription lvg = LvgAnnotator.createAnnotatorDescription();
		builder.add(lvg);
		/******************/	
		
		AnalysisEngineDescription context = ContextDependentTokenizerAnnotator.createAnnotatorDescription();
		builder.add(context);
		/******************/
		  
		AnalysisEngineDescription pos = POSTagger.createAnnotatorDescription();
		builder.add(pos);
		/******************/
	  
		AnalysisEngineDescription chunker = Chunker.createAnnotatorDescription();
		builder.add(chunker);
		/******************/
		

		AnalysisEngineDescription ChunkAdjuster =ClinicalPipelineFactory.getStandardChunkAdjusterAnnotator();
		builder.add(ChunkAdjuster);
		/******************/
	    
	    
		AnalysisEngineDescription copyNPchunks =AnalysisEngineFactory.createEngineDescription( CopyNPChunksToLookupWindowAnnotations.class );
		builder.add(copyNPchunks);
		/******************/
		
		
		AnalysisEngineDescription removeEnclosed =AnalysisEngineFactory.createEngineDescription( RemoveEnclosedLookupWindows.class );
		builder.add(removeEnclosed);
		/******************/
		
		
		AnalysisEngineDescription dictionarylookup_desc = AnalysisEngineFactory.createEngineDescription( DefaultJCasTermAnnotator.class,
	               AbstractJCasTermAnnotator.PARAM_WINDOW_ANNOT_KEY,
	               "org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation",
	               JCasTermAnnotator.DICTIONARY_DESCRIPTOR_KEY,
	               "org/apache/ctakes/dictionary/lookup/fast/sno_rx_16ab.xml");
		builder.add(dictionarylookup_desc);
		
		//org/apache/ctakes/dictionary/lookup/fast/sno_rx_16ab.xml
		//org/apache/ctakes/dictionary/lookup/LookupDesc_Db.xml
		/******************/
		AnalysisEngineDescription filtersites =AnalysisEngineFactory.createEngineDescription( FilterPrepositionalObjectSites.class );
		builder.add(filtersites);
		/******************/
		
		
		
		//  builder.add( AnalysisEngineFactory.createEngineDescription(
		//			"desc/ctakes-dictionary-lookup-fast/desc/analysis_engine/UmlsOverlapLookupAnnotator") );
	    AnalysisEngineDescription myDrugMentionAnnotator = AnalysisEngineFactory.createEngineDescription(
					"desc/ctakes-drug-ner/desc/analysis_engine/myDrugMentionAnnotator") ;
		builder.add(myDrugMentionAnnotator);
		/******************/
	     

		AnalysisEngineDescription depedency =ClearNLPDependencyParserAE.createAnnotatorDescription();
		builder.add(depedency);
		/******************/
		
	
		
		AnalysisEngineDescription polarity =PolarityCleartkAnalysisEngine.createAnnotatorDescription();
		builder.add(polarity);
		/******************/
		
		
		AnalysisEngineDescription uncertainity =UncertaintyCleartkAnalysisEngine.createAnnotatorDescription();
		builder.add(uncertainity);
		/******************/
	    

		AnalysisEngineDescription semanticrole = AnalysisEngineFactory.createEngineDescription( ClearNLPSemanticRoleLabelerAE.class );
		builder.add(semanticrole);
		/******************/
		
		
		AnalysisEngineDescription ConstituencyParser = AnalysisEngineFactory.createEngineDescription( ConstituencyParser.class ) ;
		builder.add(ConstituencyParser);
		/******************/
	 	
	      
		AnalysisEngineDescription time_BackwardsAnnotator =BackwardsTimeAnnotator.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/timeannotator/model.jar");
		builder.add(time_BackwardsAnnotator);
		/******************/
			
			
		AnalysisEngineDescription time_EventAnnotator =EventAnnotator
				.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/eventannotator/model.jar");
		builder.add(time_EventAnnotator);
		/******************/	
		
		
		AnalysisEngineDescription time_TemporalEventAnnotator =AnalysisEngineFactory.createEngineDescription( CopyPropertiesToTemporalEventAnnotator.class );
		builder.add(time_TemporalEventAnnotator);
		/******************/
		
		
		AnalysisEngineDescription eventMention =AnalysisEngineFactory.createEngineDescription(AddEvent.class);
		builder.add(eventMention);
		/******************/

			
	/*	AnalysisEngineDescription time_DocTimeRelAnnotator =DocTimeRelAnnotator
   					.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/doctimerel/model.jar");
		builder.add(time_DocTimeRelAnnotator);
	*/	/******************/
		
			
			

		AnalysisEngineDescription time_EventTimeSelfRelationAnnotator =EventTimeSelfRelationAnnotator
						.createEngineDescription("/org/apache/ctakes/temporal/ae/eventtime/model.jar");
		builder.add(time_EventTimeSelfRelationAnnotator);
		/******************/
			
			
			

	    AnalysisEngineDescription time_EventEventRelationAnnotator =EventEventRelationAnnotator
					.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/eventevent/model.jar");
			builder.add(time_EventEventRelationAnnotator);
		/******************/
		
			
			
		 AnalysisEngineDescription smoking =AnalysisEngineFactory.createEngineDescription("desc/ctakes-smoking-status/desc/analysis_engine/ClassifiableEntriesAnnotator");	
		    
			builder.add(smoking);
		/******************/	
			
			  
		AnalysisEngineDescription degreeof =AnalysisEngineFactory.createEngineDescription( "desc/ctakes-relation-extractor/desc/analysis_engine/DegreeOfRelationExtractorAnnotator") ;
		builder.add(degreeof);
		/******************/
		
		
		AnalysisEngineDescription locationof =AnalysisEngineFactory.createEngineDescription( "desc/ctakes-relation-extractor/desc/analysis_engine/LocationOfRelationExtractorAnnotator") ;
		builder.add(locationof);
		/******************/
		
		
	/*	AnalysisEngineDescription template =AnalysisEngineFactory.createEngineDescription(TemplateFillerAnnotator.class);
		builder.add(template);
	*/	/******************/
			
	
		AnalysisEngineDescription extracprep =AnalysisEngineFactory.createEngineDescription("desc/ctakes-clinical-pipeline/desc/analysis_engine/ExtractionPrepAnnotator");
		builder.add(extracprep);
	   /*******************/	
		
	//	AnalysisEngineDescription engine = builder.createAggregateDescription();
		
	//	SofaMapping[] s=engine.getSofaMappings();
		
		
		
		return  builder.createAggregateDescription();
	}


	
	
	
	public static AnalysisEngineDescription getCda2Pipeline() throws ResourceInitializationException, UIMAException, IOException{
		   
		   
		   return AnalysisEngineFactory.createEngineDescription(
					"desc/ctakes-clinical-pipeline/desc/analysis_engine/AggregateCdaUMLSProcessor");
	}
	
//__________________________________________________________________________________//	
	
	public static class AddEvent extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {
		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for (EventMention emention : Lists.newArrayList(JCasUtil.select(
					jCas,
					EventMention.class))) {
				EventProperties eventProperties = new org.apache.ctakes.typesystem.type.refsem.EventProperties(jCas);

				// create the event object
				Event event = new Event(jCas);

				// add the links between event, mention and properties
				event.setProperties(eventProperties);
				emention.setEvent(event);

				// add the annotations to the indexes
				eventProperties.addToIndexes();
				event.addToIndexes();
			}
		}
	}
}