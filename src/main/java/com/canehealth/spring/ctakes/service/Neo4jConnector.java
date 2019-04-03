package com.canehealth.spring.ctakes.service;

import org.apache.ctakes.core.config.ConfigParameterConstants;
import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.DotLogger;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 1/16/2018
 */
@PipeBitInfo(
      name = "Neo4jConnector",
      description = "Connects to neo4j session on initialization.",
      role = PipeBitInfo.Role.SPECIAL
)
final public class Neo4jConnector extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "Neo4jConnector" );

   static private final String OUTPUT_GRAPH_DB = "output_graph/deepphe.db";

   /**
    * Name of configuration parameter that must be set to the path of a directory into which the
    * output files will be written.
    */
  /* @ConfigurationParameter(
         name = ConfigParameterConstants.PARAM_OUTPUTDIR,
         description = ConfigParameterConstants.DESC_OUTPUTDIR
   )
   private File _outputRootDir;*/


   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
   
   }
   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      // Do nothing
	   
	   LOGGER.info( "Loading Graph ..." );
	 //     try ( DotLogger dotLogger = new DotLogger() ) {
	      //   if ( _outputRootDir == null ) {
	         try {
				int success=   /*Neo4jPopulation2*/Neo4jPopulationDiseaseOriented.getInstance()
				                          .insertNodes(jCas);
			} catch (CASException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      //   } else {
	        //	 Neo4jPopulation2.getInstance()
	          //                        .insertNodes(jCas);
	      //   }
	   //   } catch ( IOException ioE ) {
	         // Do nothing
	    //  }
	   
	   
   }

}
