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
import ca.uhn.fhir.model.dstu.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.valueset.ObservationReliabilityEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationStatusEnum;
import ca.uhn.fhir.model.primitive.DateDt;
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
	    String obsDate = null;
	    int count = 0;
	    try {
			//Class.forName(driverName);
			String URL = url + "/" + dbName;
			conn = DriverManager.getConnection(URL, username, password);
			stmt = conn.createStatement();
			String sql = "SELECT observation_id, observation_value, observation_date, observation_concept_id FROM observation WHERE person_id= " + userInfo.personId;
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()){
				obsVal = rs.getString("observation_value");
				obsConceptId = rs.getString("observation_concept_id");
				obsId = rs.getString("observation_id");
				obsDate = rs.getString("observation_date");
				FhirContext ctx = new FhirContext();
				Observation obs = new Observation();
				obs.setId(userInfo.userId + "-"+count+"-"+ obsId);
				obs.setName(new CodeableConceptDt("http://loinc.org",obsConceptId)); 
				StringDt val = new StringDt(obsVal);
				obs.setValue(val);
				//obs.setComments("Body Weight");// if required, do -> if(Id[2] ==""){ set as ""} else{}
				obs.setComments(obsDate);
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
		ArrayList<Condition> retVal = new ArrayList<Condition>();
		Connection conn = null;
	    Statement stmt = null;    
	    //String condVal = null;
	    String condConceptId = null;
	    String condId = null;
	    String condDate = null;
	    String condName = null;
	    int count = 0;
	    try {
			//Class.forName(driverName);
			String URL = url + "/" + dbName;
			conn = DriverManager.getConnection(URL, username, password);
			stmt = conn.createStatement();
			String sql = "SELECT condition_occurrence_id, condition_start_date, condition_id, condition_name FROM condition_occurrence WHERE person_id= " + userInfo.personId;
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()){
				//condVal = rs.getString("observation_value");
				condConceptId = rs.getString("condition_id");
				condId = rs.getString("condition_occurrence_id");
				condDate = rs.getString("condition_start_date");
				condName = rs.getString("condition_name");
				FhirContext ctx = new FhirContext();
				Condition cond = new Condition();
				cond.setId(userInfo.userId+"-"+count+"-"+condId);
				ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+userInfo.userId);
				cond.setSubject(subj);
				cond.setNotes(condDate);
				CodeableConceptDt value = new CodeableConceptDt();
				value.setText(condName);
				cond.setCode(value);
				cond.addIdentifier("ICD9", condConceptId);
				//DateDt date = new DateDt(condDate);
				//setDateAsserted(date);
				ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			    String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(cond);
			    cond.getText().setDiv(output);
				//count = count+1;
				retVal.add(cond);
			}
		} catch (SQLException se) {
			// TODO Auto-generated catch block
			se.printStackTrace();
			}
		return retVal;
	}

	public ArrayList<MedicationPrescription> getMedicationPrescriptions(HealthPortUserInfo userInfo) {
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
	    String obsDate = null;
	    //String personId = null;
	    String URL = null;
	    String sql = null; 
	    ResultSet rs = null;
	    Observation obs = new Observation();
	    try {
			URL = url + "/" + dbName;
			conn = DriverManager.getConnection(URL, username, password);
			stmt = conn.createStatement();
			sql = "SELECT observation_id, observation_value, observation_date, observation_concept_id FROM observation WHERE observation_id= " + Ids[2];
			rs = stmt.executeQuery(sql);
			while(rs.next()){
				obsVal = rs.getString("observation_value");
				obsConceptId = rs.getString("observation_concept_id");		
				obsDate = rs.getString("observation_date");
				int count = 0;
				FhirContext ctx = new FhirContext();	
				obs.setId(Ids[0] + "-"+count+"-"+ Ids[2]);
				obs.setName(new CodeableConceptDt("http://loinc.org",obsConceptId)); 
				StringDt val = new StringDt(obsVal);
				obs.setValue(val);
			    obs.setComments(obsDate);
				obs.setStatus(ObservationStatusEnum.FINAL);
				obs.setReliability(ObservationReliabilityEnum.OK);
				ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			    String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
			    obs.getText().setDiv(output);
			}			
		} catch (SQLException se) {
			se.printStackTrace();
			}

		return obs;
	    
	}

	public Condition getCondition(String resourceId) {
		String[] Ids  = resourceId.split("\\-",3);
    	//Ids[0] -> person id
		//Ids[1] -> count 
		//Ids[2] -> concept id -> eg. 8302-2 for height
	    Connection conn = null;
	    Statement stmt = null;
	    String condConceptId = null;
	    //String condId = null;
	    String condDate = null;
	    String condName = null;
	    int count = 0;
	    String URL = null;
	    String sql = null; 
	    ResultSet rs = null;
	    Condition cond = new Condition();
	    try {
	    	URL = url + "/" + dbName;
			conn = DriverManager.getConnection(URL, username, password);
			stmt = conn.createStatement();
			sql = "SELECT condition_start_date, condition_id, condition_name FROM condition_occurrence WHERE condition_occurrence_id= " + Ids[2];
			rs = stmt.executeQuery(sql);
			while(rs.next()){
				condConceptId = rs.getString("condition_id");
				//condId = rs.getString("condition_occurrence_id");
				condDate = rs.getString("condition_start_date");
				condName = rs.getString("condition_name");
				FhirContext ctx = new FhirContext();
				cond.setId(Ids[0]+"-"+count+"-"+Ids[2]);
				ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+Ids[0]);
				cond.setSubject(subj);
				cond.setNotes(condDate);
				CodeableConceptDt value = new CodeableConceptDt();
				value.setText(condName);
				cond.setCode(value);
				cond.addIdentifier("ICD9", condConceptId);
				//DateDt date = new DateDt(condDate);
				//setDateAsserted(date);
				ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			    String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(cond);
			    cond.getText().setDiv(output);
			}			
		} catch (SQLException se) {
			se.printStackTrace();
			}

		return cond;
	}

	
}
