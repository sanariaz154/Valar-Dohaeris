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

import org.apache.ctakes.assertion.medfacts.cleartk.ConditionalCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.GenericCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.HistoryCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.PolarityCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.SubjectCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.UncertaintyCleartkAnalysisEngine;
import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.relationextractor.pipelines.*;
import org.apache.ctakes.template.filler.ae.TemplateFillerAnnotator;
import org.apache.ctakes.relationextractor.ae.*;
import org.apache.ctakes.clinicalpipeline.ClinicalPipelineFactory;
import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
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

import com.canehealth.spring.ctakes.service.Neo4jConnector;
import com.canehealth.spring.ctakes.service.Sectionizer;
import com.canehealth.spring.ctakes.service.myCDASegmentAnnotator;
import com.canehealth.spring.ctakes.service.myDrugMentionAnnotator;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.preprocessor.ae.CdaCasInitializer;
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
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import org.apache.uima.analysis_engine.metadata.SofaMapping; 

import com.google.common.collect.Lists;

//import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations.GenderAnnotation;
//import edu.stanford.nlp.ling.CoreAnnotations;
public class MyPipeline {	
     
	//Sana's Edit: Relation pipeline
	public static AnalysisEngineDescription getRelationBuilder() throws Exception {
	
		AggregateBuilder builder = new AggregateBuilder();
		// builder.add( AnalysisEngineFactory.createEngineDescription( RelationExtractorAnnotator.class ) );	
	  //   builder.add( AnalysisEngineFactory.createEngineDescription( DegreeOfRelationExtractorAnnotator.class ) );
	   //  builder.add( AnalysisEngineFactory.createEngineDescription( LocationOfRelationExtractorAnnotator.class ) );

		
	//	builder.add( AnalysisEngineFactory.createEngineDescription( "desc/ctakes-relation-extractor/desc/analysis_engine/DegreeOfRelationExtractorAnnotator") );
	//	builder.add( AnalysisEngineFactory.createEngineDescription( "desc/ctakes-relation-extractor/desc/analysis_engine/LocationOfRelationExtractorAnnotator") );
		builder.add( AnalysisEngineFactory.createEngineDescription( "desc/ctakes-template-filler/desc/analysis_engine/AggregateTemplateFiller") );
		
		return  builder.createAggregateDescription();
	}
	// --
	//Sana's Edit : Temp
	public static AnalysisEngineDescription getSmoking() throws Exception {
		return	AnalysisEngineFactory.createEngineDescription("desc/ctakes-smoking-status/desc/analysis_engine/ClassifiableEntriesAnnotator");
		//return  AnalysisEngineFactory.createEngineDescription(
		//		"desc/ctakes-smoking-status/desc/analysis_engine/SimulatedProdSmokingTAE" );
		}
	
	public static AnalysisEngineDescription getAggregateBuilder() throws Exception {
		AggregateBuilder builder = new AggregateBuilder();
		//builder.add(ClinicalPipelineFactory.getFastPipeline());
//	      builder.add( ClinicalPipelineFactory.getTokenProcessingPipeline() );
	//	 builder.add(AnalysisEngineFactory.createEngineDescription( CdaCasInitializer.class ));
	     
	//	  builder.add(AnalysisEngineFactory.createEngineDescription(SimpleSegmentAnnotator.class));
		builder.add(AnalysisEngineFactory.createEngineDescription(Sectionizer.class/*myCDASegmentAnnotator.class*/));
		
		
		
		  builder.add( SentenceDetector.createAnnotatorDescription() );
	      builder.add( TokenizerAnnotatorPTB.createAnnotatorDescription() );
	      builder.add( LvgAnnotator.createAnnotatorDescription() );
	      builder.add( ContextDependentTokenizerAnnotator.createAnnotatorDescription() );
	      builder.add( POSTagger.createAnnotatorDescription() );
	      builder.add( Chunker.createAnnotatorDescription() );
	      builder.add( ClinicalPipelineFactory.getStandardChunkAdjusterAnnotator() );
	      builder.add( AnalysisEngineFactory.createEngineDescription( CopyNPChunksToLookupWindowAnnotations.class ) );
	      builder.add( AnalysisEngineFactory.createEngineDescription( RemoveEnclosedLookupWindows.class ) );
	      builder.add( AnalysisEngineFactory.createEngineDescription( DefaultJCasTermAnnotator.class,
	               AbstractJCasTermAnnotator.PARAM_WINDOW_ANNOT_KEY,
	               "org.apache.ctakes.typesystem.type.textspan.Sentence",
	               JCasTermAnnotator.DICTIONARY_DESCRIPTOR_KEY,
	               "org/apache/ctakes/dictionary/lookup/fast/sno_rx_16ab.xml")
	      );	
		// new edit {drugner}	
	      
	      builder.add( AnalysisEngineFactory.createEngineDescription(
					"desc/ctakes-drug-ner/desc/analysis_engine/myDrugMentionAnnotator") );
	      //builder.add( AnalysisEngineFactory.createEngineDescription( myDrugMentionAnnotator.class ) );	
	//      builder.add( AnalysisEngineFactory.createEngineDescription( RelationExtractorAnnotator.class ) );	
	     
	      // pre-req for all assertion cleartk attribute
	      builder.add( ClearNLPDependencyParserAE.createAnnotatorDescription() );
	    //  builder.add( AnalysisEngineFactory.createEngineDescription("org/apache/ctakes/necontexts/desc/NegationAnnotator" ));
	      builder.add( ConditionalCleartkAnalysisEngine.createAnnotatorDescription() );
	      builder.add( HistoryCleartkAnalysisEngine.createAnnotatorDescription() );	
	      builder.add( GenericCleartkAnalysisEngine.createAnnotatorDescription() );
	      builder.add( SubjectCleartkAnalysisEngine.createAnnotatorDescription() );
	    //  builder.add( ClearNLPDependencyParserAE.createAnnotatorDescription() );
	      builder.add( PolarityCleartkAnalysisEngine.createAnnotatorDescription() );
	      builder.add( UncertaintyCleartkAnalysisEngine.createAnnotatorDescription() );
	      
	//      builder.add( AnalysisEngineFactory.createEngineDescription( GenderAnnotation.class ) );
	      
	      
	      builder.add( AnalysisEngineFactory.createEngineDescription( ClearNLPSemanticRoleLabelerAE.class ) );
	      builder.add( AnalysisEngineFactory.createEngineDescription( ConstituencyParser.class ) );
	      
	      // new edit {relation}
	   //   builder.add( AnalysisEngineFactory.createEngineDescription( DegreeOfRelationExtractorAnnotator.class ) );
	    //  builder.add( AnalysisEngineFactory.createEngineDescription( LocationOfRelationExtractorAnnotator.class ) );
	     
	      
	     // builder.add( AnalysisEngineFactory.createEngineDescription( RelationExtractorConsumer.class ) );
	     	// Add BackwardsTimeAnnotator
			builder.add(BackwardsTimeAnnotator
					.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/timeannotator/model.jar"));
			// Add EventAnnotator
			builder.add(EventAnnotator
					.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/eventannotator/model.jar"));
			builder.add( AnalysisEngineFactory.createEngineDescription( CopyPropertiesToTemporalEventAnnotator.class ) );
			// Add Document Time Relative Annotator
			//link event to eventMention
			builder.add(AnalysisEngineFactory.createEngineDescription(AddEvent.class));
			builder.add(DocTimeRelAnnotator
   					.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/doctimerel/model.jar"));
			// Add Event to Event Relation Annotator
			builder.add(EventTimeSelfRelationAnnotator
					.createEngineDescription("/org/apache/ctakes/temporal/ae/eventtime/model.jar"));
			// Add Event to Event Relation Annotator
			builder.add(EventEventRelationAnnotator
					.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/eventevent/model.jar"));
				
			
			builder.add( AnalysisEngineFactory.createEngineDescription( "desc/ctakes-relation-extractor/desc/analysis_engine/ModifierExtractorAnnotator") );
			builder.add( AnalysisEngineFactory.createEngineDescription( "desc/ctakes-relation-extractor/desc/analysis_engine/DegreeOfRelationExtractorAnnotator") );
			builder.add( AnalysisEngineFactory.createEngineDescription( "desc/ctakes-relation-extractor/desc/analysis_engine/LocationOfRelationExtractorAnnotator") );
 				
			AnalysisEngineDescription extracprep =AnalysisEngineFactory.createEngineDescription("desc/ctakes-clinical-pipeline/desc/analysis_engine/ExtractionPrepAnnotator");
			builder.add(extracprep);
			
			builder.add( AnalysisEngineFactory.createEngineDescription(TemplateFillerAnnotator.class));
		//	builder.add( AnalysisEngineFactory.createEngineDescription( "desc/ctakes-template-filler/desc/analysis_engine/AggregateTemplateFiller") );
			builder.add( AnalysisEngineFactory.createEngineDescription(Neo4jConnector.class));
					
			
		return builder.createAggregateDescription();
	}

	public static AnalysisEngineDescription getCASBuilder() throws Exception {
		AggregateBuilder builder = new AggregateBuilder();
		 builder.add(AnalysisEngineFactory.createEngineDescription( CdaCasInitializer.class ));
		 return builder.createAggregateDescription();
			
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////
	public static AnalysisEngineDescription getCDAAggregateBuilder() throws Exception {
		AggregateBuilder builder = new AggregateBuilder();
		
	// 	AnalysisEngineDescription cdacas=AnalysisEngineFactory.createEngineDescription( CdaCasInitializer.class );
	//	SofaMapping[] s=cdacas.getSofaMappings();
	 //	String s= ((Object) cdacas).getSofaName();
		
		
		 builder.add(AnalysisEngineFactory.createEngineDescription( CdaCasInitializer.class ));
	//	SofaMapping[] sofaMappings = new SofaMapping[] { UIMAFramework.getResourceSpecifierFactory().createSofaMapping(), UIMAFramework.getResourceSpecifierFactory().createSofaMapping() };
	   	  builder.add(AnalysisEngineFactory.createEngineDescription(SimpleSegmentAnnotator.class));
		
		//sofaMappings[0].setComponentKey("SentenceDetector");
		//sofaMappings[0].setComponentSofaName("plaintext");
	//    sofaMappings[0].setAggregateSofaName("plaintext");
	    
	  //  AnalysisEngineDescription sentence_desc=SentenceDetector.createAnnotatorDescription() ;
	    //		sentence_desc.setSofaMappings(sofaMappings);
		  builder.add( SentenceDetector.createAnnotatorDescription() );
		  
		//  sofaMappings[1].setComponentKey("TokenizerAnnotatorPTB");
			//sofaMappings[0].setComponentSofaName("plaintext");
	//	    sofaMappings[1].setAggregateSofaName("plaintext");
		    
		//    AnalysisEngineDescription tokenizer_desc=TokenizerAnnotatorPTB.createAnnotatorDescription() ;
		 //   tokenizer_desc.setSofaMappings(sofaMappings);
		//	  builder.add( tokenizer_desc );
		  
	      builder.add( TokenizerAnnotatorPTB.createAnnotatorDescription() );
	      builder.add( LvgAnnotator.createAnnotatorDescription() );
	      builder.add( ContextDependentTokenizerAnnotator.createAnnotatorDescription() );
	      builder.add( POSTagger.createAnnotatorDescription() );
	      builder.add( Chunker.createAnnotatorDescription() );
	      builder.add( ClinicalPipelineFactory.getStandardChunkAdjusterAnnotator() );
	      builder.add( AnalysisEngineFactory.createEngineDescription( CopyNPChunksToLookupWindowAnnotations.class ) );
	      builder.add( AnalysisEngineFactory.createEngineDescription( RemoveEnclosedLookupWindows.class ) );
	    
	      AnalysisEngineDescription dictionarylookup_desc = AnalysisEngineFactory.createEngineDescription( DefaultJCasTermAnnotator.class,
	               AbstractJCasTermAnnotator.PARAM_WINDOW_ANNOT_KEY,
	               "org.apache.ctakes.typesystem.type.textspan.Sentence",
	               JCasTermAnnotator.DICTIONARY_DESCRIPTOR_KEY,
	               "org/apache/ctakes/dictionary/lookup/fast/sno_rx_16ab.xml");
	      //dictionarylookup_desc.getSofaMappings();
	      builder.add(dictionarylookup_desc);
	      
	//     builder.add( UmlsDictionaryLookupAnnotator.createAnnotatorDescription());
	   //   builder.add(AnalysisEngineFactory.createEngineDescription("desc/ctakes-dictionary-lookup-fast/desc/analysis_engine/UmlsLookupAnnotator"));
	     
	      AnalysisEngineDescription myDrugMentionAnnotator = AnalysisEngineFactory.createEngineDescription(
					"desc/ctakes-drug-ner/desc/analysis_engine/myDrugMentionAnnotator") ;
	      builder.add(myDrugMentionAnnotator );
	  		
	      builder.add( ClearNLPDependencyParserAE.createAnnotatorDescription() );
	      builder.add( PolarityCleartkAnalysisEngine.createAnnotatorDescription() );
	      builder.add( UncertaintyCleartkAnalysisEngine.createAnnotatorDescription() );
	 //     builder.add( AnalysisEngineFactory.createEngineDescription( ClearNLPSemanticRoleLabelerAE.class ) );
	  
	      AnalysisEngineDescription ConstituencyParser = AnalysisEngineFactory.createEngineDescription( ConstituencyParser.class ) ;
	  //    builder.add( ConstituencyParser);
	      
	   
/*	      
	     // builder.add( AnalysisEngineFactory.createEngineDescription( RelationExtractorConsumer.class ) );
	     	// Add BackwardsTimeAnnotator
			builder.add(BackwardsTimeAnnotator
					.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/timeannotator/model.jar"));
			// Add EventAnnotator
			builder.add(EventAnnotator
					.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/eventannotator/model.jar"));
			builder.add( AnalysisEngineFactory.createEngineDescription( CopyPropertiesToTemporalEventAnnotator.class ) );
			// Add Document Time Relative Annotator
			//link event to eventMention
			builder.add(AnalysisEngineFactory.createEngineDescription(AddEvent.class));
			builder.add(DocTimeRelAnnotator
   					.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/doctimerel/model.jar"));
			// Add Event to Event Relation Annotator
			builder.add(EventTimeSelfRelationAnnotator
					.createEngineDescription("/org/apache/ctakes/temporal/ae/eventtime/model.jar"));
			// Add Event to Event Relation Annotator
			builder.add(EventEventRelationAnnotator
					.createAnnotatorDescription("/org/apache/ctakes/temporal/ae/eventevent/model.jar"));
			*/
						

			
		//	builder.add( AnalysisEngineFactory.createEngineDescription( "desc/ctakes-relation-extractor/desc/analysis_engine/DegreeOfRelationExtractorAnnotator") );
		//	builder.add( AnalysisEngineFactory.createEngineDescription( "desc/ctakes-relation-extractor/desc/analysis_engine/LocationOfRelationExtractorAnnotator") );
				
	//		builder.add( AnalysisEngineFactory.createEngineDescription(TemplateFillerAnnotator.class));
		//		builder.add(AnalysisEngineFactory.createEngineDescription(
			//		"desc/ctakes-clinical-pipeline/desc/analysis_engine/ExtractionPrepAnnotator"));		
		
		return builder.createAggregateDescription();
	}
	
	public static AnalysisEngineDescription getCda2Pipeline() throws ResourceInitializationException, UIMAException, IOException{
		   
		   
		   return AnalysisEngineFactory.createEngineDescription(
					"desc/ctakes-clinical-pipeline/desc/analysis_engine/AggregateCdaUMLSProcessor");
	}
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