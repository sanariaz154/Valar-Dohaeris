# ctakes-knowledge-engineering
KB generation of clinical data using cTAKES NLP and Neo4j . This is a getting-statred/demo repository of complete project. This project is in phase 1 of development and still in progress. 

## Example 

## Pre-Req
* Understanding of UIMA and cTAKES archetecture
* Configure cTAKES java pipeline in your system and download all resources required for ctakes. More details can be found on [cTAKES developer guide](https://cwiki.apache.org/confluence/display/CTAKES/cTAKES+3.2+Developer+Install+Guide).

## Notes
* Resources folder is deleted from this repository due to large dictionary files and inconsistent jars. The zip file of all resources will be shared through google drive link later. 
* Change the Neo4j database path according to your system from _Neo4JPopulation.java_ (or _Neo4jPopulationDiseaseOriented.java_ , according to case)
* If processing CCDA documents, set _is_ccda=true_ in _cTAKESService.java_
* If processing multiple documents, set _is_CPE=true_ in _cTAKESService.java_ and send documents directory path through postman.
* To get data from multiple healthcare sites for processing, run crawlers  or simply download the documents already extracted given in [med-harvest](https://github.com/sanariaz154/med-harvest) repository and you will get more than enough data in documents form for testing.

## Framework



