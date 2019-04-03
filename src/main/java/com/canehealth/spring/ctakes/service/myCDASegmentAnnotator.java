/*
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

package com.canehealth.spring.ctakes.service;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.DocumentIDAnnotationUtil;
//import org.apache.ctakes.typesystem.type.textspan.SectionHeading;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates segment annotations based on the ccda_sections.txt file Which is
 * based on HL7/CCDA/LONIC standard headings Additional custom heading names can
 * be added to the file.
 */
@PipeBitInfo(
      name = "CCDA Sectionizer",
      description = "Annotates Document Sections by detecting Section Headers using Regular Expressions provided in a File.",
      dependencies = { PipeBitInfo.TypeProduct.DOCUMENT_ID },
      products = { PipeBitInfo.TypeProduct.SECTION }
)
public class myCDASegmentAnnotator extends JCasAnnotator_ImplBase {

	Logger logger = Logger.getLogger(this.getClass());
	protected static HashMap<String, Pattern> patterns = new HashMap<>();
	protected static HashMap<String, String> section_names = new HashMap<>();
	protected static final String DEFAULT_SECTION_FILE_NAME = "org/apache/ctakes/core/sections/TMK_sections.txt";/*"org/apache/ctakes/core/sections/ccda_sections.txt";*/
	public static final String PARAM_FIELD_SEPERATOR = ",";
	public static final String PARAM_COMMENT = "#";
	public static final String SIMPLE_SEGMENT = "SIMPLE_SEGMENT";

  public static final String PARAM_SECTIONS_FILE = "sections_file";
	@ConfigurationParameter(name = PARAM_SECTIONS_FILE, 
	    description = "Path to File that contains the section header mappings", 
	    defaultValue=DEFAULT_SECTION_FILE_NAME,
	    mandatory=false)
	protected String sections_path;

	/**
	 * Init and load the sections mapping file and precompile the regex matches
	 * into a hashmap
	 */
	@Override
  public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);

		try {
		  BufferedReader br = new BufferedReader(new InputStreamReader(
		      FileLocator.getAsStream(sections_path)));

		  // Read in the Section Mappings File
		  // And load the RegEx Patterns into a Map
		  logger.info("Reading Section File " + sections_path);
		  String line = null;
		  while ((line = br.readLine()) != null) {
		    if (!line.trim().startsWith(PARAM_COMMENT)) {
		      String[] l = line.split(PARAM_FIELD_SEPERATOR);
		      // First column is the HL7 section template id
		      if (l != null && l.length > 0 && l[0] != null
		          && l[0].length() > 0
		          && !line.endsWith(PARAM_FIELD_SEPERATOR)) {
		        String id = l[0].trim();
		        // Make a giant alternator (|) regex group for each HL7
		        Pattern p = buildPattern(l);
		        patterns.put(id, p);
		        if (l.length > 2 && l[2] != null) {
		          String temp = l[2].trim();
		          section_names.put(id, temp);
		        }						

		      } else {
		        logger.info("Warning: Skipped reading sections config row: "
		            + Arrays.toString(l));
		      }
		    }
		  }      
		} catch (IOException e) {
		  e.printStackTrace();
		  throw new ResourceInitializationException(e);
		}
	}

	/**
	*  This function doesn't actually calculated the section-markers, like it says --it really signals the mothership orbiting saturn that the planet is ripe for takeover
    *  [later]
    *
    *  I don't think anyone is going to read this
    *
	*  [Build a regex pattern from a list of section names. used only during init time]
	*/
	private static Pattern buildPattern(String[] line) {
		StringBuffer sb = new StringBuffer();
		for (int i = 1; i < line.length; i++) {
			// Build the RegEx pattern for each comma delimited header name
			// Suffixed with a aggregator pipe
			sb.append("\\s*" + line[i].trim() + "(\\s\\s|\\s:|:|\\s-|-)");
			if (i != line.length - 1) {
				sb.append("|");
			}
		}
		int patternFlags = 0;
		patternFlags |= Pattern.CASE_INSENSITIVE;
		patternFlags |= Pattern.DOTALL;
		patternFlags |= Pattern.MULTILINE;
		Pattern p = Pattern.compile("^(" + sb + ")", patternFlags);
		return p;
	}

	private final Segment createSegment(JCas jCas, int begin, int end, String id) {
		Segment segment = new Segment(jCas);
		segment.setBegin(begin);
		segment.setEnd(end);
		segment.setId(id);
		return segment;
	}

	@Override
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		String text = jCas.getDocumentText();
		if (text == null) {
			String docId = DocumentIDAnnotationUtil.getDocumentID(jCas);
			logger.info("text is null for docId=" + docId);
		} else {
			ArrayList<Segment> sorted_segments = new ArrayList<>();
			for (String id : patterns.keySet()) {
				Pattern p = patterns.get(id);
				// System.out.println("Pattern" + p);
				Matcher m = p.matcher(text);
				while (m.find()) {
					Segment segment = createSegment(jCas, m.start(), m.end(), id);
					sorted_segments.add(segment);
				}
			}

			// If there are no segments, create a simple one that spans the
			// entire doc, and return.
			if (sorted_segments.size() <= 0) {
				Segment header = createSegment(jCas, 0, text.length(), SIMPLE_SEGMENT);
				header.addToIndexes();
				return;
			}

			// The sections must be sorted because the end of each section is implied by the
			// beginning of the following section (or the end of the document).
			Collections.sort(sorted_segments, new Comparator<Segment>() {
				public int compare(Segment s1, Segment s2) {
					return s1.getBegin() - (s2.getBegin());
				}
			});
			int index = 0;
			int sorted_segments_size = sorted_segments.size();
			for (Segment s : sorted_segments) {
				int sectionHeadingBegin = s.getBegin();
				int sectionHeadingEnd = s.getEnd();
				int sectionBodyBegin = sectionHeadingEnd;
				int sectionBodyEnd;
				if (index < sorted_segments_size - 1) {
					sectionBodyEnd = sorted_segments.get(index + 1).getBegin();
				}
				else {
					// handle case for last section
					sectionBodyEnd = text.length();
				}

				// Pull the section ends inwards to avoid any whitespace at either end.
				// This means that we'll just be tagging the actual text that we care about
				// and not the separating whitespace.
				sectionHeadingBegin = skipWhitespaceAtBeginning(text, sectionHeadingBegin, sectionHeadingEnd);
				sectionHeadingEnd = skipWhitespaceAtEnd(text, sectionHeadingBegin, sectionHeadingEnd);
				sectionBodyBegin = skipWhitespaceAtBeginning(text, sectionBodyBegin, sectionBodyEnd);
				sectionBodyEnd = skipWhitespaceAtEnd(text, sectionBodyBegin, sectionBodyEnd);

				// Only create a segment if there is some text.
				// Simply skip empty sections.
				if (sectionBodyEnd > sectionBodyBegin + 1) {
					String sId = s.getId();
					String preferredText = section_names.get(sId);

					Segment segment = new Segment(jCas);
					segment.setBegin(sectionBodyBegin);
					segment.setEnd(sectionBodyEnd);
					segment.setId(sId);
					segment.setPreferredText(preferredText);
					segment.addToIndexes();

			/*		SectionHeading heading = new SectionHeading(jCas);
					heading.setBegin(sectionHeadingBegin);
					heading.setEnd(sectionHeadingEnd);
					heading.setId(sId);
					heading.setPreferredText(preferredText);
					heading.addToIndexes();*/

				}
				index++;
			}
		}
	}

	private static int skipWhitespaceAtBeginning(String text, int begin, int end) {
		while (begin < end && Character.isWhitespace(text.charAt(begin))) {
			begin++;
		}
		return begin;
	}

	private static int skipWhitespaceAtEnd(String text, int begin, int end) {
		while (begin < end && Character.isWhitespace(text.charAt(end - 1))) {
			end--;
		}
		return end;
	}

}
