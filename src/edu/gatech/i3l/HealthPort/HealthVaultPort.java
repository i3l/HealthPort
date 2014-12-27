package edu.gatech.i3l.HealthPort;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.emf.common.util.EList;
import org.openhealthtools.mdht.uml.cda.EntryRelationship;
import org.openhealthtools.mdht.uml.cda.ccd.CCDPackage;
import org.openhealthtools.mdht.uml.cda.ccd.ContinuityOfCareDocument;
import org.openhealthtools.mdht.uml.cda.ccd.ProblemAct;
import org.openhealthtools.mdht.uml.cda.ccd.ProblemSection;
import org.openhealthtools.mdht.uml.cda.ccd.ResultObservation;
import org.openhealthtools.mdht.uml.cda.ccd.ResultOrganizer;
import org.openhealthtools.mdht.uml.cda.ccd.ResultsSection;
import org.openhealthtools.mdht.uml.cda.ccd.VitalSignsOrganizer;
import org.openhealthtools.mdht.uml.cda.ccd.VitalSignsSection;
import org.openhealthtools.mdht.uml.cda.util.CDAUtil;
import org.openhealthtools.mdht.uml.hl7.datatypes.CD;
import org.openhealthtools.mdht.uml.hl7.datatypes.CE;
import org.openhealthtools.mdht.uml.hl7.datatypes.PQ;
import org.openhealthtools.mdht.uml.hl7.datatypes.ST;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu.composite.QuantityDt;
import ca.uhn.fhir.model.dstu.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.valueset.ConditionStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationReliabilityEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationStatusEnum;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;

import com.microsoft.hsg.ConnectionFactory;
import com.microsoft.hsg.HVAccessor;
import com.microsoft.hsg.Request;

public class HealthVaultPort {

	static public String getCCD (String Record_id, String Person_id) {		
		
		if (Person_id == null || Record_id == null) {
			return null;
		}
		StringBuilder requestXml = new StringBuilder();
		requestXml.append("<info><group>");
		requestXml.append("<filter><type-id>9c48a2b8-952c-4f5a-935d-f3292326bf54</type-id></filter>");
		requestXml.append("<format><section>core</section><section>otherdata</section><xml/></format>");
		requestXml.append("</group></info>");

		Request request2 = new Request();
		request2.setMethodName("GetThings");
		request2.setOfflineUserId(Person_id);
		request2.setRecordId(Record_id);
		request2.setInfo(requestXml.toString());

		HVAccessor accessor = new HVAccessor();
		accessor.send(request2, ConnectionFactory.getConnection());
		InputStream is = accessor.getResponse().getInputStream();
		
		int i;
		char c;
		StringBuilder resString = new StringBuilder();
        
		try {
			while((i=is.read())!=-1)
			{
			   // converts integer to character
			   c=(char)i;
			   
			   // prints character
			   resString.append(c);
			   //System.out.print(c);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return  null;
		}
		
		//Remove the HV response tags
		String finalString;
		int intIndex = resString.indexOf("<Clinical");
		finalString = resString.substring(intIndex);
		intIndex = finalString.indexOf("<common>");
		finalString = finalString.substring(0, intIndex);
		//System.out.println(finalString);
		
		return finalString;	
	}
	static public String getThings (String code, String Record_id, String Person_id){
		if (Person_id == null || Record_id == null) {
			return null;
		}
		StringBuilder requestXml = new StringBuilder();
		requestXml.append("<info><group>");
		requestXml.append("<filter><type-id>"+code+"</type-id></filter>");
		requestXml.append("<format><section>core</section><section>otherdata</section><xml/></format>");
		requestXml.append("</group></info>");

		Request request2 = new Request();
		request2.setMethodName("GetThings");
		request2.setOfflineUserId(Person_id);
		request2.setRecordId(Record_id);
		request2.setInfo(requestXml.toString());

		HVAccessor accessor = new HVAccessor();
		accessor.send(request2, ConnectionFactory.getConnection());
		InputStream is = accessor.getResponse().getInputStream();
		
		int i;
		char c;
		StringBuilder resString = new StringBuilder();
        
		try {
			while((i=is.read())!=-1)
			{
			   // converts integer to character
			   c=(char)i;
			   // prints character
			   resString.append(c);
			   //System.out.print(c);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return  null;
		}
		
		return resString.toString();
	}
	
	 //Parse a given CCD (using mdht) and create observations
    static public ArrayList<Observation> getObservationByCCD(String rId, String pId,String PatientID){
    	String ccd=null;
    	ArrayList<Observation> retVal = new ArrayList<Observation>();
    	//get CCD from healthVault
    	ccd = getCCD(rId, pId);
		
		//Parsing of CCD
		CCDPackage.eINSTANCE.eClass();
		ContinuityOfCareDocument ccdDocument = null;
		ArrayList<String> observationList = new ArrayList<String>();

		try {
		InputStream is = new ByteArrayInputStream(ccd.getBytes());
		ccdDocument = (ContinuityOfCareDocument) CDAUtil.load(is);
		} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		}
		//Retrieve Results from CCD (lab tests)
		ResultsSection results = ccdDocument.getResultsSection();
		if(results!=null){
		for (ResultOrganizer resultOrganizer : results.getResultOrganizers()) {
			for (ResultObservation resultObservation : resultOrganizer.getResultObservations()) {
				observationList.add(resultObservation.getCode().getOriginalText().getText());
				if (!resultObservation.getValues().isEmpty() && resultObservation.getValues().get(0) instanceof PQ) {
					PQ value = (PQ) resultObservation.getValues().get(0);
					observationList.add((value.getValue()).toString());
					observationList.add(value.getUnit());
				}
				if (!resultObservation.getValues().isEmpty() && resultObservation.getValues().get(0) instanceof ST) {
					ST value = (ST) resultObservation.getValues().get(0);
					observationList.add(value.getText());
					observationList.add("N/A");
				}
			}
		}
		}
		//Retrieve Vitals from CCD 
		VitalSignsSection vitals = ccdDocument.getVitalSignsSection();
		if(vitals!=null){
		for (VitalSignsOrganizer vitalsOrganizer : vitals.getVitalSignsOrganizers()) {
			for (ResultObservation resultObservation : vitalsOrganizer.getResultObservations()) {
				observationList.add(resultObservation.getCode().getDisplayName());
				if (!resultObservation.getValues().isEmpty() && resultObservation.getValues().get(0) instanceof PQ) {
					PQ value = (PQ) resultObservation.getValues().get(0);
					observationList.add(value.getValue().toString());
					observationList.add(value.getUnit());
				}
				
			}
		}
		}
		//create Observations
		for (int i = 0; i < observationList.size(); i=i+3) {
			Observation obs = new Observation();
		
			obs.setId("pid:"+PatientID); // This is object resource ID. 
			String nameCode = getCode(observationList.get(i));
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(observationList.get(i+1))).setUnits(observationList.get(i+2));
			obs.setValue(quantity);
			obs.setComments(observationList.get(i));
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);
			retVal.add(obs);
		}
    	return retVal;
    }
    
    //Create Observations based on individual data calls
    static public ArrayList<Observation> getObservation(String rId, String pId,String PatientID){
    	ArrayList<Observation> retVal = new ArrayList<Observation>();
    	ArrayList<String> retList = new ArrayList<String>();
    	FhirContext ctx = new FhirContext();
    	String output = null;
    	
    	//Get the Weight and create Observations
		retList = getWeight(rId, pId);
		for (int i = 0; i < retList.size(); i=i+2) {
			Observation obs = new Observation();
		
			obs.setId("pid:"+PatientID); // This is object resource ID. 
			String nameCode = getCode("Body weight");
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i))).setUnits(retList.get(i+1));
			obs.setValue(quantity);
			obs.setComments("Weight");
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);
			
			obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
			ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
		    obs.getText().setDiv(output);
		    
			retVal.add(obs);
		}
		//Get the Height and create Observations
		retList.clear();
		retList=getHeight(rId, pId);
		for (int i = 0; i < retList.size(); i=i+2) {
			Observation obs = new Observation();
			obs.setId("pid:"+PatientID); // This is object resource ID. 
			String nameCode = "0000";
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i))).setUnits(retList.get(i+1));
			obs.setValue(quantity);
			obs.setComments("Height");
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);
			
			obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
			ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
		    obs.getText().setDiv(output);
		    
			retVal.add(obs);
		}
		//Get the blood Pressure and create Observations
		retList.clear();
		retList = getBloodPressure(rId, pId);
		for (int i = 0; i < retList.size(); i=i+3) {
			Observation obs = new Observation();
			obs.setId("pid:"+PatientID); // This is object resource ID. 
			String nameCode = "0000";
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i+1))).setUnits(retList.get(i+2));
			obs.setValue(quantity);
			obs.setComments(retList.get(i));
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);
			
			obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
			ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
		    obs.getText().setDiv(output);
		    
			retVal.add(obs);
		}
		//Get the Lab Results and create Observations
		retList.clear();
		retList = getLabResults(rId, pId);
		for (int i = 0; i < retList.size(); i=i+4) {
			Observation obs = new Observation();
			obs.setId("pid:"+PatientID); // This is object resource ID. 
			String nameCode = "0000";
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i+2))).setUnits(retList.get(i+3));
			obs.setValue(quantity);
			obs.setComments(retList.get(i+1));
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);
			
			obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
			ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
		    obs.getText().setDiv(output);
		    
			retVal.add(obs);
		}
		retList.clear();
		retList = getBloodGlucose(rId, pId);
		for (int i = 0; i < retList.size(); i=i+3) {
			Observation obs = new Observation();
			obs.setId("pid:"+PatientID); // This is object resource ID. 
			String nameCode = "0000";
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i))).setUnits(retList.get(i+1));
			obs.setValue(quantity);
			obs.setComments("Glucose in " + retList.get(i+2));
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);
			
			obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
			ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
		    obs.getText().setDiv(output);
		    
			retVal.add(obs);
		}
		retList.clear();
		retList = getCholesterol(rId, pId);
		for (int i = 0; i < retList.size(); i=i+2) {
			Observation obs = new Observation();
			obs.setId("pid:"+PatientID); // This is object resource ID. 
			String nameCode = "0000";
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i))).setUnits(retList.get(i+1));
			obs.setValue(quantity);
			obs.setComments("Cholesterol");
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);
			
			obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
			ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
		    obs.getText().setDiv(output);
		    
			retVal.add(obs);
		}
    	return retVal;
    }
    
	//If given a CCD, parse and create Conditions
    /*
	 static public ArrayList<Condition> getHVConditionByCCD(String rId, String pId,int patientNum){
		 ArrayList<String> conditionList = new ArrayList<String>();
		 ArrayList<Condition> retVal = new ArrayList<Condition>(); 
		 String ccd=null;
		 ccd = HealthVaultPort.getCCD(rId, pId);
			
		//Parsing of CCD
		CCDPackage.eINSTANCE.eClass();
		ContinuityOfCareDocument ccdDocument = null;
		
		try {
		InputStream is = new ByteArrayInputStream(ccd.getBytes());
		ccdDocument = (ContinuityOfCareDocument) CDAUtil.load(is);
		} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		}
	
		ProblemSection problem = ccdDocument.getProblemSection();
		if(problem!=null){
		EList<ProblemAct> problemActs = problem.getProblemActs();

		for(int i =0; i < problemActs.size(); i++){
			EList<Observation> probOb = problemActs.get(i).getObservations();
			CD value = (CD) probOb.get(0).getValues().get(0); // DataType, CD from CCD file
			conditionList.add(value.getDisplayName());

			// Status we need is from entryRelationship
			for (EntryRelationship entryRel : probOb.get(0).getEntryRelationships()) {
				CD codeV = entryRel.getObservation().getCode();
				if (codeV.getCodeSystem().equals("2.16.840.1.113883.6.1")) {
					if (codeV.getCode().equals("33999-4")) {
						// Here we need to compare the status and map to FHIR status
						// provisional
						//This is a tentative diagnosis - still a candidate that is under consideration.
						// working
						//The patient is being treated on the basis that this is the condition, but it is still not confirmed.
						// confirmed
						//There is sufficient diagnostic and/or clinical evidence to treat this as a confirmed condition.
						// refuted
						//This condition has been ruled out by diagnostic and clinical evidence.
						// For active, we use confirmed. 
						// Snomed code for ProblemStatus can be obtained from
						// https://art-decor.org/art-decor/decor-valuesets--ccd1-?valueSetRef=2.16.840.1.113883.1.11.20.13

						CE statV = (CE) entryRel.getObservation().getValues().get(0);
						//System.out.println ("code system: "+statV.getCodeSystem()+", code: "+statV.getCode()+", display: "+statV.getDisplayName());
						conditionList.add(statV.getCodeSystem());
						conditionList.add(statV.getCode());
						
					}
				}
			}
		}
		}
	   		for (int i = 0; i < conditionList.size(); i=i+3) {
   			Condition cond = new Condition();
   			cond.setId("pid:"+patientNum);
   			ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+patientNum);
   			cond.setSubject(subj);
   			CodeableConceptDt value = new CodeableConceptDt();
   			// We have to put system and code. This should be obtained from CCD. 
   			// If not found like coag CCD, then we don't put anything as this is 
   			// not a required field.
   			// Instead, we put text.
   			value.setText(conditionList.get(i));
   			cond.setCode(value );
   			//cond.setStatus(ConditionStatusEnum.CONFIRMED);
   			if (conditionList.get(i+1).equals("2.16.840.1.113883.1.11.20.13")) {
					if (conditionList.get(i+2).equals("55561003")) {
						// Active
						cond.setStatus(ConditionStatusEnum.CONFIRMED);
					} else if (conditionList.get(i+2).equals("73425007")) {
						// Inactive
						cond.setStatus(ConditionStatusEnum.REFUTED);
						//System.out.println("Put refuted to FHIR status");
					} else if (conditionList.get(i+2).equals("90734009")) {
						// Chronic
						cond.setStatus(ConditionStatusEnum.CONFIRMED);
						//System.out.println("Put confirmed to FHIR status");
					} else if (conditionList.get(i+2).equals("7087005")) {
						// Intermittent
						cond.setStatus(ConditionStatusEnum.WORKING);
						//System.out.println("Put working to FHIR status");
					} else if (conditionList.get(i+2).equals("255227004")) {
						// Recurrent
						cond.setStatus(ConditionStatusEnum.WORKING);
						//System.out.println("Put working to FHIR status");
					} else if (conditionList.get(i+2).equals("415684004")) {
						// Rule out
						cond.setStatus(ConditionStatusEnum.REFUTED);
						//System.out.println("Put refuted to FHIR status");
					} else if (conditionList.get(i+2).equals("410516002")) {
						// Ruled out
						cond.setStatus(ConditionStatusEnum.REFUTED);
						//System.out.println("Put refuted to FHIR status");
					} else if (conditionList.get(i+2).equals("413322009")) {
						// Resolved
						cond.setStatus(ConditionStatusEnum.CONFIRMED);
						//System.out.println("Put refuted to FHIR status");
					} 
				}

   			retVal.add(cond);
   		}
	   		return retVal;
		 
		 
	 }
    */
    
    static public ArrayList<Condition> getHVCondition(String rId, String pId,int patientNum){
    	ArrayList<String> conditionList = new ArrayList<String>();
    	ArrayList<Condition> retVal = new ArrayList<Condition>();   
    	FhirContext ctx = new FhirContext();
		String output = null;
    	
		//retVal = HVCCDParseCondition(rId, pId,patientNum);
		conditionList = HealthVaultPort.getCondition(rId, pId);
		for (int i = 0; i < conditionList.size(); i=i+2) {
			Condition cond = new Condition();
			cond.setId("pid:"+patientNum);
			ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+patientNum);
			cond.setSubject(subj);
			CodeableConceptDt value = new CodeableConceptDt();
			value.setText(conditionList.get(i));
			cond.setCode(value );
			if (conditionList.get(i+1).equals("active")) {
				// Active
				cond.setStatus(ConditionStatusEnum.CONFIRMED);
			} else if (conditionList.get(i+1).equals("inactive")) {
				// Inactive
				cond.setStatus(ConditionStatusEnum.REFUTED);
				//System.out.println("Put refuted to FHIR status");
			} else if (conditionList.get(i+1).equals("chronic")) {
				// Chronic
				cond.setStatus(ConditionStatusEnum.CONFIRMED);
				//System.out.println("Put confirmed to FHIR status");
			} else if (conditionList.get(i+1).equals("intermittent")) {
				// Intermittent
				cond.setStatus(ConditionStatusEnum.WORKING);
				//System.out.println("Put working to FHIR status");
			} else if (conditionList.get(i+2).equals("recurrent")) {
				// Recurrent
				cond.setStatus(ConditionStatusEnum.WORKING);
				//System.out.println("Put working to FHIR status");
			} else if (conditionList.get(i+2).equals("rule out")) {
				// Rule out
				cond.setStatus(ConditionStatusEnum.REFUTED);
				//System.out.println("Put refuted to FHIR status");
			} else if (conditionList.get(i+2).equals("ruled out")) {
				// Ruled out
				cond.setStatus(ConditionStatusEnum.REFUTED);
				//System.out.println("Put refuted to FHIR status");
			} else if (conditionList.get(i+2).equals("resolved")) {
				// Resolved
				cond.setStatus(ConditionStatusEnum.CONFIRMED);
				//System.out.println("Put refuted to FHIR status");
			} 
			
			cond.getText().setStatus(NarrativeStatusEnum.GENERATED);
			ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(cond);
		    cond.getText().setDiv(output);
			
			retVal.add(cond);
		}
		return retVal;
		
    }
    
    
 
	static public ArrayList<String> getWeight (String Record_id, String Person_id){
		String responseStr = null;
		String Value = null;
		String Units = null;
		//String finalString = "";
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("3d34d87e-7fc1-4153-800f-f56592cb0d17",Record_id,Person_id);
		System.out.println(responseStr);
		
		   try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
				doc.getDocumentElement().normalize();
				NodeList nList = doc.getElementsByTagName("weight");
				
				for (int temp = 0; temp < nList.getLength(); temp++) {
					Node nNode = nList.item(temp);
					
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						Value = eElement.getElementsByTagName("value").item(0).getTextContent();
						Units = null;
						if (eElement.getElementsByTagName("kg")!= null){
							Units = "kg";
						}
						else if (eElement.getElementsByTagName("lb")!= null){
							Units = "lbs";
						}
						else{
							Units = "N/A";
						}
					}
					finalList.add(Value);
					finalList.add(Units);
				}
			    } catch (Exception e) {
				e.printStackTrace();
			    }
		return finalList;
		
	}
	static public ArrayList<String> getHeight (String Record_id, String Person_id){
		String responseStr = null;
		String Value = null;
		String Units = null;
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("40750a6a-89b2-455c-bd8d-b420a4cb500b",Record_id,Person_id);
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("height");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					Value = eElement.getElementsByTagName("value").item(0).getTextContent();
					Units = null;
					if (eElement.getElementsByTagName("m")!= null){
						Units = "m";
					}
					else if (eElement.getElementsByTagName("ft")!= null){
						Units = "ft";
					}
					else{
						Units = "N/A";
					}
				}
				finalList.add(Value);
				finalList.add(Units);
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		return finalList ;	
	}
	static public ArrayList<String> getBloodPressure(String Record_id, String Person_id){
		String responseStr = null;
		String systolic = null;
		String diastolic = null;
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("ca3c57f4-f4c1-4e15-be67-0a3caf5414ed",Record_id,Person_id);
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("blood-pressure");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					systolic = eElement.getElementsByTagName("systolic").item(0).getTextContent();
					diastolic= eElement.getElementsByTagName("diastolic").item(0).getTextContent();
				}
				finalList.add("Systolic Blood Pressure");
				finalList.add(systolic);
				finalList.add("mm[Hg]");
				finalList.add("Diastolic Blood Pressure");
				finalList.add(diastolic);
				finalList.add("mm[Hg]");
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		return finalList;	
	}
	static public ArrayList<String> getBloodGlucose(String Record_id, String Person_id){
		String responseStr = null;
		String value = null;
		String unit = null;
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("879e7c04-4e8a-4707-9ad3-b054df467ce4",Record_id,Person_id);
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("blood-glucose");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					nNode = nNode.getFirstChild().getNextSibling().getFirstChild();
					value = nNode.getTextContent();
					finalList.add(value);
					nNode = nNode.getNextSibling();
					unit = nNode.getTextContent();
					if (value.equals(unit)){
						unit = "mmol/L";
					}
					else{
						unit = "mg/dL";
					}
					finalList.add(unit);
					nNode = nNode.getParentNode().getNextSibling().getFirstChild();
					finalList.add(nNode.getTextContent());
				}
				
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		return finalList;	
	}
	static public ArrayList<String> getCholesterol(String Record_id, String Person_id){
		String responseStr = null;
		String value = null;
		String unit = null;
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("98f76958-e34f-459b-a760-83c1699add38",Record_id,Person_id);
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("cholesterol-profile");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					nNode = nNode.getFirstChild().getNextSibling().getNextSibling().getNextSibling().getFirstChild();
					value = nNode.getTextContent();
					finalList.add(value);
					nNode = nNode.getNextSibling();
					unit = "mmol/L";
					finalList.add(unit);
				}
				
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		return finalList;	
	}
	static public ArrayList<String> getLabResults(String Record_id, String Person_id){
		String responseStr = null;
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("5800eab5-a8c2-482a-a4d6-f1db25ae08c3",Record_id,Person_id);
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("results");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					finalList.add(eElement.getElementsByTagName("date").item(0).getTextContent());
					nNode = nNode.getFirstChild().getNextSibling();
					finalList.add(nNode.getTextContent());
					nNode = nNode.getNextSibling();
					eElement = (Element) nNode;
					String[] split = (eElement.getElementsByTagName("display").item(0).getTextContent()).split("\\s+");
					if (split.length == 2){
						finalList.add(split[0]);
						finalList.add(split[1]);
					}
					else{
						finalList.add(split[0]);
						finalList.add("N/A");
					}
				}
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		
		return finalList;	
	}

	
	static public ArrayList<String> getCondition(String Record_id, String Person_id){
		String responseStr = null;
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("7ea7a1f9-880b-4bd4-b593-f5660f20eda8",Record_id,Person_id);
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("condition");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					finalList.add(eElement.getElementsByTagName("name").item(0).getTextContent());
					nNode = nNode.getFirstChild().getNextSibling().getFirstChild().getNextSibling();
					eElement = (Element) nNode;
					finalList.add(eElement.getElementsByTagName("value").item(0).getTextContent());
				}
				
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		
		return finalList;
		
	}
	
	static public ArrayList<String> getMedication(String Record_id, String Person_id){
		String responseStr = null;
	
		ArrayList<String> finalList = new ArrayList<String>();
		responseStr= getThings("30cafccc-047d-4288-94ef-643571f7919d",Record_id,Person_id);
		System.out.println(responseStr);
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("medication");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					finalList.add(eElement.getElementsByTagName("name").item(0).getTextContent());
					nNode = nNode.getFirstChild().getNextSibling().getFirstChild();
					String[] split = (nNode.getTextContent().split("\\s+"));
					finalList.add(split[0]);
					finalList.add(split[1]);
					nNode = nNode.getParentNode().getNextSibling().getFirstChild();
					split = (nNode.getTextContent().split("\\s+"));
					finalList.add(split[0]);
					finalList.add(split[1]);
				}
				
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		
		System.out.println(finalList);
		return finalList;
	}
	 static public String getCode(String name) {
		    String lcode = null;
	    	if (name.equals("Blood Urea Nitrogen")){
	    		lcode = "49071-4";
	    	}
	    	if (name.equals("Creatinine Test")){
	    		lcode = "30004-6";
	    	}
	    	if (name.equals("PT (Prothrombin Time)")){
	    		lcode = "5894-1";
	    	}
	    	if (name.equals("INR (International Normalized Ratio)")){
	    		lcode = "72281-9";
	    	}
	    	if (name.equals("Glomerular Filtration Rate (GFR)")){
	    		lcode = "69405-9";
	    	}
	    	if (name.equals("Hemoglobin A1C (HbA1C)")){
	    		lcode = "55454-3";
	    	}
	    	if (name.equals("Compression ultrasonography")){
	    		lcode = "000";
	    	}
	    	if (name.equals("CAT Scan")){
	    		lcode = "35884-6";
	    	}
	    	if (name.equals("Body weight")){
	    		lcode = "3141-9";
	    	}
	 
	    	return lcode;
		}

}
