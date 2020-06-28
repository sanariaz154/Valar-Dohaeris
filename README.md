# ctakes-knowledge-engineering
KB generation of clinical data using cTAKES NLP and Neo4j . This is a getting-statred/demo repository of complete project. This project is in phase 1 of development and still in progress. 

## Example 

#### Example 1

> A 43-year-old woman was diagnosed with type 2 diabetes mellitus by her family physician 3 months before this presentation. Her initial blood glucose was 340 mg/dL.       Glyburide 2.5 mg once daily was prescribed. Since then, self-monitoring of blood glucose (SMBG) showed blood glucose levels of 250-270 mg/dL. She was referred to an endocrinologist for further evaluation. She had extreme pain in her left kidney.
On examination, she was normotensive and not acutely ill. Her body mass index (BMI) was 18.7 kg/m2 following a recent 10 lb weight loss. Her thyroid was symmetrically enlarged and ankle reflexes absent. Her blood glucose was 272 mg/dL, and her hemoglobin A1c (HbA1c) was 10.3%. A lipid profile showed a total cholesterol of 261 mg/dL, triglyceride level of 321 mg/dL, HDL level of 48 mg/dL, and an LDL of 150 mg/dL. Thyroid function was normal. Urinanalysis showed trace ketones. 
She adhered to a regular exercise program and vitamin regimen, smoked 2 packs of cigarettes daily for the past 25 years, and limited her alcohol intake to 1 drink daily. Her mother's brother was diabetic. 

**_CEM Annotations_**

<img src="https://github.com/sanariaz154/Valar-Dohaeris/blob/master/imgs/cem-annotations-ex2.PNG?raw=true" />

**_Neo4j Graph Ontology_**

<img src="https://github.com/sanariaz154/Valar-Dohaeris/blob/master/imgs/ex2.png?raw=true" />

#### Example 2 
Following text is the medication section of a Clinical Discharge Summary taken from mtsamples.com:
> **MEDICATIONS ON TRANSFER**: 
>  - Aspirin 325 mg once a day. 
>  - Metoprolol 50 mg once a day, but we have had to hold it because of relative bradycardia which he apparently has a history of. 
>  - Nexium 40 mg once a day. 
>  - Zocor 40 mg once a day, and there is a fasting lipid profile pending at the time of this dictation.  I see that his LDL was 136 on May 3, 2002. 
>  - Plavix 600 mg p.o. x1 which I am giving him tonight.

**_Neo4j Graph Ontology_**
<img src="https://github.com/sanariaz154/Valar-Dohaeris/blob/master/imgs/ex1.png?raw=true" />

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

<img src="https://github.com/sanariaz154/Valar-Dohaeris/blob/master/imgs/framewok.PNG?raw=true" />



## Phase 2

In phase-2 of development, we will be applying different graph-ml algorithm (like decision trees) on available data in graph to get a good accuracy of diagnosis. This phase will also be focusing on Knowledge Validation, Inferencing and Explanation/ Justification process of knowledge Engineering to maintain our healthcare knowledge base. Currently we do not have CCDA’s available with information of symptom of patients (chief complaint, reason for visit etc.). For the time being, we are collecting data from online sources and will use that to symptom based disease diagnosis.



