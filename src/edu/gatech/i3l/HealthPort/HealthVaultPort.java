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
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.valueset.ConditionStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationReliabilityEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationStatusEnum;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;

import com.microsoft.hsg.ConnectionFactory;
import com.microsoft.hsg.HVAccessor;
import com.microsoft.hsg.Request;

public class HealthVaultPort implements HealthPortFHIRIntf {

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
	
	static public String getThingsById (String thing_id, String Record_id, String Person_id){
		if (Person_id == null || Record_id == null) {
			return null;
		}
		System.out.println(thing_id);
		String id = "a712ca78-68b5-4df2-8f3e-0f94b6e7fb61";
		StringBuilder requestXml = new StringBuilder();
		requestXml.append("<info><group>");
		requestXml.append("<id>"+thing_id+"</id>");
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
	
	//Get Observation by Id
	public Observation getObservation(String resourceId){
		Observation obs = new Observation();
		String responseStr = null;
		ArrayList<String> retList = new ArrayList<String>();
		ArrayList<Observation> retVal = new ArrayList<Observation>();
		Observation finalRetVal = new Observation();
		String type = null;
		String[] Ids  = resourceId.split("\\-",3);
    	
    	HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(Integer.parseInt(Ids[0]));
    	String rId = HealthPortUser.recordId;
    	String pId = HealthPortUser.personId;
    	
    	responseStr = getThingsById(Ids[2],rId, pId);
    	
    	try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			
			NodeList nList = doc.getElementsByTagName("type-id");
			Node nNode = nList.item(0);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				type = nNode.getTextContent();
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
    	//Weight type
    	if (type.equals("3d34d87e-7fc1-4153-800f-f56592cb0d17")){
    		retList = getWeight(responseStr,rId,pId);
    		retVal = setWeightObservation(Ids[0],retList,retVal);
    	}
    	//Height type
    	if (type.equals("40750a6a-89b2-455c-bd8d-b420a4cb500b")){
    		retList = getHeight(responseStr,rId,pId);
    		retVal = setHeightObservation(Ids[0],retList,retVal);
    	}
    	//Blood Pressure type
    	if (type.equals("ca3c57f4-f4c1-4e15-be67-0a3caf5414ed")){
    		retList = getBloodPressure(responseStr,rId,pId);
    		retVal = setBloodPressObservation(Ids[0],retList,retVal);
    	}
    	//Blood Glucose type
    	if (type.equals("879e7c04-4e8a-4707-9ad3-b054df467ce4")){
    		retList = getBloodGlucose(responseStr,rId,pId);
    		retVal = setBloodGlucoseObservation(Ids[0],retList,retVal);
    	}
    	//Cholesterol type
    	if (type.equals("98f76958-e34f-459b-a760-83c1699add38")){
    		retList = getCholesterol(responseStr,rId,pId);
    		retVal = setCholesterolObservation(Ids[0],retList,retVal);
    	}
    	//Lab results type
    	if (type.equals("5800eab5-a8c2-482a-a4d6-f1db25ae08c3")){
    		retList = getLabResults(responseStr,rId,pId);
    		retVal = setLabObservation(Ids[0],retList,retVal);
    	}
    	Integer index = Integer.parseInt(Ids[1]);
    	if (!index.equals(0)){
    		finalRetVal = retVal.get(index);
    	}
    	else{
    		finalRetVal = retVal.get(0);
    	}
		return finalRetVal;
	
	}
	
    //Create Observations based on individual data calls
    public ArrayList<Observation> getObservations(HealthPortUserInfo userInfo){
    	ArrayList<Observation> retVal = new ArrayList<Observation>();
    	ArrayList<String> retList = new ArrayList<String>();
    	String response = "temp response";
    	
    	//Get the Weight and create Observations
		retList = getWeight(response,userInfo.recordId, userInfo.personId);
		retVal = setWeightObservation(userInfo.userId,retList,retVal);
		
		//Get the Height and create Observations
		retList.clear();
		retList=getHeight(response, userInfo.recordId, userInfo.personId);
		retVal = setHeightObservation(userInfo.userId,retList,retVal);
		
		//Get the blood Pressure and create Observations
		retList.clear();
		retList = getBloodPressure(response, userInfo.recordId, userInfo.personId);
		retVal = setBloodPressObservation(userInfo.userId,retList,retVal);
		
		//Get the blood Glucose and create Observations
		retList.clear();
		retList = getBloodGlucose(response, userInfo.recordId, userInfo.personId);
		retVal = setBloodGlucoseObservation(userInfo.userId,retList,retVal);
		
		//Get the Cholesterol and create Observations
		retList.clear();
		retList = getCholesterol(response, userInfo.recordId, userInfo.personId);
		retVal = setCholesterolObservation(userInfo.userId,retList,retVal);
		
		//Get the Lab Results and create Observations
		retList.clear();
		retList = getLabResults(response, userInfo.recordId, userInfo.personId);
		retVal = setLabObservation(userInfo.userId,retList,retVal);
		
    	return retVal;
    }
    
  //Get Condition by Id
  	public Condition getCondition(String resourceId){
  		Condition cond = new Condition();
  		String responseStr = null;
  		ArrayList<String> retList = new ArrayList<String>();
  		ArrayList<Condition> retVal = new ArrayList<Condition>();
  		Condition finalRetVal = new Condition();
  		String type = null;
  		String[] Ids  = resourceId.split("\\-",3);
      	
      	HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(Integer.parseInt(Ids[0]));
      	String rId = HealthPortUser.recordId;
      	String pId = HealthPortUser.personId;
      	
      	responseStr = getThingsById(Ids[2],rId, pId);
      	
      	try {
  			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
  			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
  			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
  			doc.getDocumentElement().normalize();
  			
  			NodeList nList = doc.getElementsByTagName("type-id");
  			Node nNode = nList.item(0);
  			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
  				type = nNode.getTextContent();
  			}
  		    } catch (Exception e) {
  			e.printStackTrace();
  		    }
      	//Condition type
      	if (type.equals("7ea7a1f9-880b-4bd4-b593-f5660f20eda8")){
    		retList = getConditionRequest(responseStr,rId,pId);
    		retVal = setConditionObservation(Ids[0],retList,retVal);
    	}
      	finalRetVal = retVal.get(0);
      	
  		return finalRetVal;
  	
  	}
    
    public ArrayList<Condition> getConditions(HealthPortUserInfo userInfo){
    	ArrayList<String> conditionList = new ArrayList<String>();
    	ArrayList<Condition> retVal = new ArrayList<Condition>();   
    	String response = "temp response";
    	
		conditionList = getConditionRequest(response,userInfo.recordId, userInfo.personId);
		retVal = setConditionObservation(userInfo.userId,conditionList,retVal);
		
		return retVal;
		
    }
    
	public ArrayList<MedicationPrescription> getMedicationPrescriptions(HealthPortUserInfo userInfo) {
		ArrayList<MedicationPrescription> retVal = new ArrayList<MedicationPrescription>();
    	ArrayList<String> retList = new ArrayList<String>();
    	String response = "temp response";
    	
    	retList = getMedication(response,userInfo.recordId, userInfo.personId);
    	
    	
    	
    	
		return retVal;
	}
    
	static public ArrayList<String> getWeight (String responseStr,String Record_id, String Person_id){
		String Value = null;
		String Units = null;
		ArrayList<String> finalList = new ArrayList<String>();
		
		if(responseStr.equals("temp response")){
			responseStr= getThings("3d34d87e-7fc1-4153-800f-f56592cb0d17",Record_id,Person_id);
		}

		   try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
				doc.getDocumentElement().normalize();
				
				NodeList nList = doc.getElementsByTagName("thing");
				for (int temp = 0; temp < nList.getLength(); temp++) {
					Node nNode = nList.item(temp);
					
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						finalList.add(nNode.getFirstChild().getTextContent());
						
						Element eElement = (Element) nNode;
						Value = eElement.getElementsByTagName("weight").item(0).getFirstChild().getNextSibling().getTextContent();
						finalList.add(Value);
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
						finalList.add(Units);
					}
				
				}
			    } catch (Exception e) {
				e.printStackTrace();
			    }
		return finalList;
		
	}
	static public ArrayList<Observation> setWeightObservation(String userId, ArrayList<String> retList, ArrayList<Observation> retVal){
		int count = 0;
		FhirContext ctx = new FhirContext();
		for (int i = 0; i < retList.size(); i=i+3) {
		Observation obs = new Observation();
		obs.setId(userId + "-"+count+"-"+ retList.get(i));
		String nameCode = getCode("Body weight");
		obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
		QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i+1))).setUnits(retList.get(i+2));
		obs.setValue(quantity);
		obs.setComments("Body Weight");
		obs.setStatus(ObservationStatusEnum.FINAL);
		obs.setReliability(ObservationReliabilityEnum.OK);
		ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
	    String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
	    obs.getText().setDiv(output);
	  
		retVal.add(obs);
		}
		return retVal;
	}
	static public ArrayList<String> getHeight (String responseStr, String Record_id, String Person_id){
		String Value = null;
		String Units = null;
		ArrayList<String> finalList = new ArrayList<String>();
		if(responseStr.equals("temp response")){
			responseStr= getThings("40750a6a-89b2-455c-bd8d-b420a4cb500b",Record_id,Person_id);
		}
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("thing");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					finalList.add(nNode.getFirstChild().getTextContent());
					
					Element eElement = (Element) nNode;
					Value = eElement.getElementsByTagName("height").item(0).getFirstChild().getNextSibling().getTextContent();
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
		return finalList;	
	}
	static public ArrayList<Observation> setHeightObservation(String userId, ArrayList<String> retList, ArrayList<Observation> retVal){
		int count = 0;
		FhirContext ctx = new FhirContext();
		for (int i = 0; i < retList.size(); i=i+3) {
			Observation obs = new Observation();
			obs.setId(userId+"-"+count+"-"+retList.get(i)); // This is object resource ID. 
			String nameCode = "0000";
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i+1))).setUnits(retList.get(i+2));
			obs.setValue(quantity);
			obs.setComments("Height");
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);
			ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		    String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
		    obs.getText().setDiv(output);
		  
			retVal.add(obs);
		}
		return retVal;
	}
	static public ArrayList<String> getBloodPressure(String responseStr,String Record_id, String Person_id){
		String systolic = null;
		String diastolic = null;
		String id = null;
		ArrayList<String> finalList = new ArrayList<String>();
		if(responseStr.equals("temp response")){
			responseStr= getThings("ca3c57f4-f4c1-4e15-be67-0a3caf5414ed",Record_id,Person_id);
		}
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("thing");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					id = nNode.getFirstChild().getTextContent();
					Element eElement = (Element) nNode;
					systolic = eElement.getElementsByTagName("systolic").item(0).getTextContent();
					diastolic= eElement.getElementsByTagName("diastolic").item(0).getTextContent();
				}
				finalList.add(id);
				finalList.add("Systolic Blood Pressure");
				finalList.add(systolic);
				finalList.add("mm[Hg]");
				finalList.add(systolic + "/"+diastolic);
				finalList.add(id);
				finalList.add("Diastolic Blood Pressure");
				finalList.add(diastolic);
				finalList.add("mm[Hg]");
				finalList.add(systolic + "/"+diastolic);
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		return finalList;	
	}
	static public ArrayList<Observation> setBloodPressObservation(String userId, ArrayList<String> retList, ArrayList<Observation> retVal){
		int count = 0;
		FhirContext ctx = new FhirContext();
		for (int i = 0; i < retList.size(); i=i+5) {
			Observation obs = new Observation();
			obs.setId(userId+"-"+count+"-"+retList.get(i)); // This is object resource ID. 
			String nameCode = "0000";
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i+2))).setUnits(retList.get(i+3));
			obs.setValue(quantity);
			obs.setComments(retList.get(i+1) + ", Overall:"+ retList.get(i+4));
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);
			ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		    String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
		    obs.getText().setDiv(output);
			if (retList.get(i+1).equals("Systolic Blood Pressure")){
				count = count+1;
			}
			else{
				count = 0;
			}
			retVal.add(obs);
		}
		return retVal;
	}
	static public ArrayList<String> getBloodGlucose(String responseStr,String Record_id, String Person_id){
		String value = null;
		String unit = null;
		String id = null;
		ArrayList<String> finalList = new ArrayList<String>();
		if(responseStr.equals("temp response")){
			responseStr= getThings("879e7c04-4e8a-4707-9ad3-b054df467ce4",Record_id,Person_id);
		}
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("thing");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					id = nNode.getFirstChild().getTextContent();
					finalList.add(id);
					nNode = nNode.getLastChild().getFirstChild();
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
	static public ArrayList<Observation> setBloodGlucoseObservation(String userId, ArrayList<String> retList, ArrayList<Observation> retVal){
		int count = 0;
		FhirContext ctx = new FhirContext();
		for (int i = 0; i < retList.size(); i=i+4) {
			Observation obs = new Observation();
			obs.setId(userId+"-"+count+"-"+retList.get(i)); // This is object resource ID. 
			String nameCode = "0000";
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i+1))).setUnits(retList.get(i+2));
			obs.setValue(quantity);
			obs.setComments("Glucose in " + retList.get(i+3));
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);
			ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		    String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
		    obs.getText().setDiv(output);
		   
			retVal.add(obs);
		}
		return retVal;
	}
	static public ArrayList<String> getCholesterol(String responseStr,String Record_id, String Person_id){
		String value = null;
		String unit = null;
		String id = null;
		ArrayList<String> finalList = new ArrayList<String>();
		if(responseStr.equals("temp response")){
			responseStr= getThings("98f76958-e34f-459b-a760-83c1699add38",Record_id,Person_id);
		}
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("thing");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					id = nNode.getFirstChild().getTextContent();
					finalList.add(id);
					nNode = nNode.getLastChild().getFirstChild();
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
	
	static public ArrayList<Observation> setCholesterolObservation(String userId, ArrayList<String> retList, ArrayList<Observation> retVal){
		int count =0;
		FhirContext ctx = new FhirContext();
		for (int i = 0; i < retList.size(); i=i+3) {
			Observation obs = new Observation();
			obs.setId(userId+"-"+count+"-"+retList.get(i)); // This is object resource ID. 
			String nameCode = "0000";
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i+1))).setUnits(retList.get(i+2));
			obs.setValue(quantity);
			obs.setComments("Cholesterol");
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);
			ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		    String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
		    obs.getText().setDiv(output);
			retVal.add(obs);
		}
		return retVal;
	}
	static public ArrayList<String> getLabResults(String responseStr,String Record_id, String Person_id){
		String id = null;
		String testSet = null;
		ArrayList<String> finalList = new ArrayList<String>();
		if(responseStr.equals("temp response")){
			responseStr= getThings("5800eab5-a8c2-482a-a4d6-f1db25ae08c3",Record_id,Person_id);
		}
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("results");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					
					testSet = nNode.getParentNode().getFirstChild().getTextContent();
					id = nNode.getParentNode().getParentNode().getParentNode().getParentNode().getFirstChild().getTextContent();
					Element eElement = (Element) nNode;
					finalList.add(id);
					finalList.add(testSet);
					//finalList.add(eElement.getElementsByTagName("date").item(0).getTextContent());
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
	static public ArrayList<Observation> setLabObservation(String userId, ArrayList<String> retList, ArrayList<Observation> retVal){
		int count =0;
		FhirContext ctx = new FhirContext();
		for (int i = 0; i < retList.size(); i=i+5) {
			Observation obs = new Observation();
			obs.setId(userId+"-"+count+"-"+retList.get(i)); // This is object resource ID. 
			String nameCode = "0000";
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i+3))).setUnits(retList.get(i+4));
			obs.setValue(quantity);
			obs.setComments(retList.get(i+2)+" from: "+retList.get(i+1));
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);
			ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
			obs.getText().setDiv(output);
			count = count+1;	    
			retVal.add(obs);
		}
		return retVal;
	}
	
	static public ArrayList<String> getConditionRequest(String responseStr,String Record_id, String Person_id){
		String id = null;
		ArrayList<String> finalList = new ArrayList<String>();
		if (responseStr.equals("temp response")){
			responseStr= getThings("7ea7a1f9-880b-4bd4-b593-f5660f20eda8",Record_id,Person_id);
		}
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("thing");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					id = nNode.getFirstChild().getTextContent();
					finalList.add(id);
					Element eElement = (Element) nNode;
					finalList.add(eElement.getElementsByTagName("name").item(0).getTextContent());
					nNode = nNode.getLastChild().getFirstChild().getFirstChild().getNextSibling();
					eElement = (Element) nNode;
					finalList.add(eElement.getElementsByTagName("value").item(0).getTextContent());
				}
				
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		
		return finalList;
		
	}
	static public ArrayList<Condition> setConditionObservation(String userId, ArrayList<String> conditionList, ArrayList<Condition> retVal){
		int count =0;
		FhirContext ctx = new FhirContext();
		for (int i = 0; i < conditionList.size(); i=i+3) {
			Condition cond = new Condition();
			cond.setId(userId+"-"+count+"-"+conditionList.get(i));
			ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+userId);
			cond.setSubject(subj);
			CodeableConceptDt value = new CodeableConceptDt();
			value.setText(conditionList.get(i+1));
			cond.setCode(value );
			if (conditionList.get(i+2).equals("active")) {
				// Active
				cond.setStatus(ConditionStatusEnum.CONFIRMED);
			} else if (conditionList.get(i+2).equals("inactive")) {
				// Inactive
				cond.setStatus(ConditionStatusEnum.REFUTED);
				//System.out.println("Put refuted to FHIR status");
			} else if (conditionList.get(i+2).equals("chronic")) {
				// Chronic
				cond.setStatus(ConditionStatusEnum.CONFIRMED);
				//System.out.println("Put confirmed to FHIR status");
			} else if (conditionList.get(i+2).equals("intermittent")) {
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
			
			ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		    String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(cond);
		    cond.getText().setDiv(output);
			count = count+1;
			retVal.add(cond);
		}
		return retVal;
	}
	
	static public ArrayList<String> getMedication(String responseStr,String Record_id, String Person_id){
	
		ArrayList<String> finalList = new ArrayList<String>();
		if(responseStr.equals("temp response")){
			responseStr= getThings("30cafccc-047d-4288-94ef-643571f7919d",Record_id,Person_id);
		}
		System.out.println(responseStr);
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new InputSource(new StringReader(responseStr)));
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("thing");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					finalList.add(nNode.getFirstChild().getTextContent());
					//System.out.println(nNode.getFirstChild().getTextContent());
					Element eElement = (Element) nNode;
					finalList.add(eElement.getElementsByTagName("name").item(0).getTextContent());
					//System.out.println(eElement.getElementsByTagName("name").item(0).getTextContent());
					nNode = nNode.getLastChild().getFirstChild().getFirstChild().getNextSibling().getFirstChild();
					String[] split = (nNode.getTextContent().split("\\s+"));
					finalList.add(split[0]);
					//System.out.println(split[0]);
					finalList.add(split[1]);
					//System.out.println(split[1]);
					nNode = nNode.getParentNode().getNextSibling().getFirstChild();
					split = (nNode.getTextContent().split("\\s+"));
					finalList.add(split[0]);
					//System.out.println(split[0]);
					finalList.add(split[1]);
					//System.out.println(split[1]);
					nNode = nNode.getParentNode().getNextSibling().getFirstChild();
					finalList.add(nNode.getTextContent());
					//System.out.println(nNode.getTextContent());
					nNode = nNode.getParentNode().getNextSibling().getFirstChild();
					finalList.add(nNode.getTextContent());
					//System.out.println(nNode.getTextContent());
					nNode = nNode.getParentNode().getNextSibling().getFirstChild();
					finalList.add(nNode.getTextContent());
					//System.out.println(nNode.getTextContent());
					nNode = nNode.getParentNode().getNextSibling().getNextSibling().getNextSibling().getFirstChild();
					if (nNode.getTextContent().equals("Prescribed")){
						nNode=nNode.getParentNode().getNextSibling().getFirstChild().getFirstChild();
						finalList.add(nNode.getTextContent());
						//System.out.println(nNode.getTextContent());
					}
					else{
						finalList.add("N/A");
						//System.out.println("N/A");
					}
				}
				
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		
		//System.out.println(finalList);
		return finalList;
	}
	static public ArrayList<MedicationPrescription> setMedications(String userId, ArrayList<String> retList, ArrayList<MedicationPrescription> retVal){
		//MedicationPrescription med = new MedicationPrescription();
		int count =0;
		for (int i = 0; i < retList.size(); i=i+10) {
			MedicationPrescription med = new MedicationPrescription();
			med.setId(userId+"-"+count+"-"+retList.get(i)); // This is object resource ID. 
			String nameCode = "0000";
			
			
			//obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
			//ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		    //output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
		    //obs.getText().setDiv(output);
		    //count = count+1;
			retVal.add(med);
		}
		
		
		
		return retVal;
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

	
	 //Parse a given CCD (using mdht) and create observations
	/*
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
    */
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

}
