/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu.composite.QuantityDt;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.valueset.ObservationReliabilityEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationStatusEnum;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;

/**
 * @author MC142
 *
 */
public class SyntheticEHRPort implements HealthPortFHIRIntf {
	
	String serverName = "localhost";
	String url = "jdbc:mysql://" + serverName;
	String username = "healthport";
	String password = "i3lworks";
	String dbName = "OMOP";

	public ArrayList<Observation> getObservations(HealthPortUserInfo userInfo) {
		// TODO Auto-generated method stub
		ArrayList<Observation> retVal = new ArrayList<Observation>();
    	//ArrayList<String> retList = new ArrayList<String>();    	
    	//Get all Observations
	    Connection conn = null;
	    Statement stmt = null;    
	    String obsVal = null;
	    String obsConceptId = null;
	    String obsId = null;
	    int count = 0;
	    try {
			//Class.forName(driverName);
			String URL = url + "/" + dbName;
			conn = DriverManager.getConnection(URL, username, password);
			stmt = conn.createStatement();
			String sql = "SELECT observation_id, observation_value, observation_concept_id FROM observation WHERE person_id= " + userInfo.personId;
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()){
				obsVal = rs.getString("observation_value");
				obsConceptId = rs.getString("observation_concept_id");
				obsId = rs.getString("observation_id");
				FhirContext ctx = new FhirContext();
				Observation obs = new Observation();
				obs.setId(userInfo.personId + "-"+count+"-"+ obsId);
				obs.setName(new CodeableConceptDt("http://loinc.org",obsConceptId)); 
				StringDt val = new StringDt(obsVal);
				obs.setValue(val);
				//obs.setComments("Body Weight");// if required, do -> if(Id[2] ==""){ set as ""} else{}
				obs.setStatus(ObservationStatusEnum.FINAL);
				obs.setReliability(ObservationReliabilityEnum.OK);
				ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			    String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
			    obs.getText().setDiv(output);
				//finalRetVal.add(obs);
				//return obs;
				retVal.add(obs);
			}
		} catch (SQLException se) {
			// TODO Auto-generated catch block
			se.printStackTrace();
			}
		return retVal;
	}

	public ArrayList<Condition> getConditions(HealthPortUserInfo userInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	public ArrayList<MedicationPrescription> getMedicationPrescriptions(
			HealthPortUserInfo userInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	public Observation getObservation(String resourceId) {
		//ArrayList<String> obsList = new ArrayList<String>();
		String[] Ids  = resourceId.split("\\-",3);
    	//Ids[0] -> person id
		//Ids[1] -> count 
		//Ids[2] -> concept id -> eg. 8302-2 for height
	    Connection conn = null;
	    Statement stmt = null;
	    String obsVal = null;
	    String obsConceptId = null;
	    try {
			String URL = url + "/" + dbName;
			conn = DriverManager.getConnection(URL, username, password);
			stmt = conn.createStatement();
			String sql = "SELECT observation_value, observation_concept_id FROM observation WHERE person_id= " + Ids[0] + " and observation_id= " + Ids[2];
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()){
				obsVal = rs.getString("observation_value");
				obsConceptId = rs.getString("observation_concept_id");				
		         //retList.add(obsVal);
		         //retVal = setObservation(Ids[0],Ids[2],retList,retVal);
			}
			
		} catch (SQLException se) {
			// TODO Auto-generated catch block
			se.printStackTrace();
			}

	    int count = 0;
		FhirContext ctx = new FhirContext();
		Observation obs = new Observation();
		obs.setId(Ids[0] + "-"+count+"-"+ Ids[2]);
		obs.setName(new CodeableConceptDt("http://loinc.org",obsConceptId)); 
		//String nameCode = getCode("Body weight");
		//obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
		//QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i+1))).setUnits(retList.get(i+2));
		StringDt val = new StringDt(obsVal);
		obs.setValue(val);
		//obs.setComments("Body Weight");// if required, do -> if(Id[2] ==""){ set as ""} else{}
		obs.setStatus(ObservationStatusEnum.FINAL);
		obs.setReliability(ObservationReliabilityEnum.OK);
		ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
	    String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
	    obs.getText().setDiv(output);
		//finalRetVal.add(obs);
		return obs;
	    
	}
	
	static public ArrayList<Observation> setObservation(String userId, String nameCode, ArrayList<String> retList, ArrayList<Observation> retVal){
		int count = 0;
		FhirContext ctx = new FhirContext();
		for (int i = 0; i < retList.size(); i=i+3) {
		Observation obs = new Observation();
		obs.setId(userId + "-"+count+"-"+ retList.get(i));
		//String nameCode = getCode("Body weight");
		obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
		QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i+1))).setUnits(retList.get(i+2));
		obs.setValue(quantity);
		//obs.setComments("Body Weight");
		obs.setStatus(ObservationStatusEnum.FINAL);
		obs.setReliability(ObservationReliabilityEnum.OK);
		ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
	    String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
	    obs.getText().setDiv(output);
	  
		retVal.add(obs);
		}
		return retVal;
	}

	public Condition getCondition(String resourceId) {
		// TODO Auto-generated method stub
		String[] data = resourceId.split("-");
        System.out.println(data[1]);
		return null;
	}

	
}
