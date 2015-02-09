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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu.composite.CodingDt;
import ca.uhn.fhir.model.dstu.composite.QuantityDt;
import ca.uhn.fhir.model.dstu.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.valueset.ConditionStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationReliabilityEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationStatusEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
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
	String dbName2 = "HealthPort";
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMdd");

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
				//FhirContext ctx = new FhirContext();
				Observation obs = new Observation();
				obs.setId(userInfo.userId + "-"+count+"-"+ obsId);
				obs.setName(new CodeableConceptDt("http://loinc.org",obsConceptId)); 
				StringDt val = new StringDt(obsVal);
				obs.setValue(val);
				//obs.setComments("Body Weight");// if required, do -> if(Id[2] ==""){ set as ""} else{}
				obs.setComments(obsDate);
				ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+userInfo.userId);
				obs.setSubject(subj);
				String obsType = null;
				if (obsConceptId == "3141-9"){
					obsType = "Body Weight";
				}
				else if (obsConceptId.equals("8302-2")){
					obsType = "Body Height";
				}
				else if (obsConceptId.equals("9279-1")){
					obsType = "Respiration Rate";
				}
				else if (obsConceptId.equals("8867-4")){
					obsType = "Heart Beat";
				}
				else if (obsConceptId.equals("8480-6")){
					obsType = "Systolic BP";
				}
				else if (obsConceptId.equals("8462-4")){
					obsType = "Diastolic BP";
				}
				else if (obsConceptId.equals("8310-5")){
					obsType = "Body Temperature";
				}
				Date date = new Date();
				try {
					date = formatter.parse(obsDate);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				obs.setIssuedWithMillisPrecision(date);
				obs.setStatus(ObservationStatusEnum.FINAL);
				obs.setReliability(ObservationReliabilityEnum.OK);
				NarrativeStatusEnum narrative = null;
				obs.getText().setStatus(narrative.GENERATED);
				StringBuffer buffer_narrative = new StringBuffer();
				buffer_narrative.append("<div>\n");
				buffer_narrative.append("<div class=\"hapiHeaderText\">" + obsType + "</div>\n");
				buffer_narrative.append("<table class=\"hapiPropertyTable\">\n");
				buffer_narrative.append("	<tbody>\n");
				buffer_narrative.append("		<tr>\n");
				buffer_narrative.append("			<td>Value</td>\n");
				buffer_narrative.append("			<td>"+ val + "</td>\n");
				buffer_narrative.append("		</tr>\n");
				buffer_narrative.append("		<tr>\n");
				buffer_narrative.append("			<td>Date</td>\n");
				buffer_narrative.append("			<td>"+ obsDate + "</td>\n");
				buffer_narrative.append("		</tr>\n");
				buffer_narrative.append("	</tbody>\n");
				buffer_narrative.append("</table>\n");
				buffer_narrative.append("</div>\n");
				String output = buffer_narrative.toString();
			    obs.getText().setDiv(output);
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
				Condition cond = new Condition();
				cond.setId(userInfo.userId+"-"+count+"-"+condId);
				ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+userInfo.userId);
				cond.setSubject(subj);
				//cond.setNotes(condDate);
				CodeableConceptDt value = new CodeableConceptDt();
				value.setText(condName);
				CodingDt code = new CodingDt();
				code.setCode(condConceptId);
				code.setSystem("http://snomed.info/sct");
				code.setDisplay(condName);
				List<CodingDt> theValue = new ArrayList<CodingDt>();
				theValue.add(code);
				value.setCoding(theValue);
				cond.setCode(value );
				Date date = new Date();
				//String finaldate = condDate.substring(0, 8);
				//System.out.println(finaldate);
				try {
					date = formatter2.parse(condDate.substring(0, 8));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				cond.setDateAssertedWithDayPrecision(date);
				NarrativeStatusEnum narrative = null;
				cond.getText().setStatus(narrative.GENERATED);
				StringBuffer buffer_narrative = new StringBuffer();		
				//cond.setCode(value);
				//cond.addIdentifier("ICD9", condConceptId);
				cond.setStatus(ConditionStatusEnum.CONFIRMED);
				buffer_narrative.append("<div>\n");
				buffer_narrative.append("<div class=\"hapiHeaderText\">" + cond.getCode().getText()+ "</div>\n");
				buffer_narrative.append("<table class=\"hapiPropertyTable\">\n");
				buffer_narrative.append("	<tbody>\n");
				buffer_narrative.append("		<tr>\n");
				buffer_narrative.append("			<td>Status</td>\n");
				buffer_narrative.append("			<td>"+ cond.getStatus().getValue() + "</td>\n");
				buffer_narrative.append("		</tr>\n");
				buffer_narrative.append("		<tr>\n");
				buffer_narrative.append("			<td>Date</td>\n");
				buffer_narrative.append("			<td>"+ date + "</td>\n");
				buffer_narrative.append("		</tr>\n");
				buffer_narrative.append("	</tbody>\n");
				buffer_narrative.append("</table>\n");
				buffer_narrative.append("</div>\n");
				String output = buffer_narrative.toString();
			    cond.getText().setDiv(output);
				//DateDt date = new DateDt(condDate);
				//setDateAsserted(date);
				//ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			    //String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(cond);
			    //cond.getText().setDiv(output);
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
		ArrayList<MedicationPrescription> retVal = new ArrayList<MedicationPrescription>();
		Connection conn = null;
	    Statement stmt = null;    
	    String[] drugDosage = null;
	    String drugName = null;
	    String lastFilled = null;
	    String drugId = null;
	    String drugConceptId = null;
	    int count = 0;
	    try {
			//Class.forName(driverName);
			String URL = url + "/" + dbName;
			conn = DriverManager.getConnection(URL, username, password);
			stmt = conn.createStatement();
			String sql = "SELECT drug_exposure_id, drug_id, drug_name, drug_dosage, last_filled_date FROM drug_exposure WHERE person_id= " + userInfo.personId;
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()){
				drugDosage = rs.getString("drug_dosage").split("\\s", 2);  
				drugId = rs.getString("drug_exposure_id");
				drugConceptId = rs.getString("drug_id");
				drugName = rs.getString("drug_name");
				lastFilled = rs.getString("last_filled_date");
				//FhirContext ctx = new FhirContext();
				MedicationPrescription med = new MedicationPrescription();
				med.setId(userInfo.userId+"-"+count+"-"+drugId); // This is object resource ID. 
				ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+userInfo.userId);
				med.setPatient(subj);
				ResourceReferenceDt medicationName = new ResourceReferenceDt();
				medicationName.setDisplay(drugName);
				
				med.setMedication(medicationName);
				ArrayList<MedicationPrescription.DosageInstruction> dosageList = new ArrayList<MedicationPrescription.DosageInstruction>();
				MedicationPrescription.DosageInstruction dosage = new MedicationPrescription.DosageInstruction(); 
				double theValue = Double.parseDouble(drugDosage[0]);
				dosage.setDoseQuantity(null, theValue, drugDosage[1]);
				dosageList.add(dosage);
				med.setDosageInstruction(dosageList);
				//yyyymmdd
				DateTimeDt date = new DateTimeDt(lastFilled.substring(0,8));
				med.setDateWritten(date);
				med.addIdentifier("NDC", drugConceptId);
				NarrativeStatusEnum narrative = null;
				med.getText().setStatus(narrative.GENERATED);
				StringBuffer buffer_narrative = new StringBuffer();
				buffer_narrative.append("<div>\n");
				buffer_narrative.append("<status value=\"generated\"/>\n");
				buffer_narrative.append("<div class=\"hapiHeaderText\">" + med.getMedication().getDisplay()+ "</div>\n");
				buffer_narrative.append("<table class=\"hapiPropertyTable\">\n");
				buffer_narrative.append("	<tbody>\n");
				buffer_narrative.append("		<tr>\n");
				buffer_narrative.append("			<td>Medication Name</td>\n");
				buffer_narrative.append("			<td>"+ med.getMedication().getDisplay() + "</td>\n");
				buffer_narrative.append("		</tr>\n");
				buffer_narrative.append("	</tbody>\n");
				buffer_narrative.append("</table>\n");
				buffer_narrative.append("</div>\n");
				//ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			   // String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(cond);
				String output = buffer_narrative.toString();
			    med.getText().setDiv(output);
			    retVal.add(med);
			}
		} catch (SQLException se) {
			// TODO Auto-generated catch block
			se.printStackTrace();
			}
		return retVal;
	}
	
	public MedicationPrescription getMedicationPrescription(String resourceId){
		MedicationPrescription med = new MedicationPrescription();
  		Connection conn = null;
	    Statement stmt = null;
	    String[] drugDosage = null;
	    String drugName = null;
	    String lastFilled = null;
	    String drugId = null;
	    String URL = null;
	    String sql = null; 
	    ResultSet rs = null;
  		//String type = null;
  		String[] Ids  = resourceId.split("\\-",3);
      	
      	//HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(Integer.parseInt(Ids[0]));
      	//String rId = HealthPortUser.recordId;
      	//String pId = HealthPortUser.personId;
	    try {
			URL = url + "/" + dbName;
			conn = DriverManager.getConnection(URL, username, password);
			stmt = conn.createStatement();
			sql = "SELECT drug_id, drug_name, drug_dosage, last_filled_date FROM drug_exposure WHERE drug_exposure_id= " + Ids[2];
			rs = stmt.executeQuery(sql);
			while(rs.next()){
				drugDosage = rs.getString("drug_dosage").split("\\s", 2);  
				drugId = rs.getString("drug_id");
				drugName = rs.getString("drug_name");
				lastFilled = rs.getString("last_filled_date");
				//FhirContext ctx = new FhirContext();	
				//SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				int count =0;
				med.setId(Ids[0]+"-"+count+"-"+Ids[2]); // This is object resource ID. 
				ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+Ids[0]);
				med.setPatient(subj);
				ResourceReferenceDt medicationName = new ResourceReferenceDt();
				medicationName.setDisplay(drugName);
				med.setMedication(medicationName);
				ArrayList<MedicationPrescription.DosageInstruction> dosageList = new ArrayList<MedicationPrescription.DosageInstruction>();
				MedicationPrescription.DosageInstruction dosage = new MedicationPrescription.DosageInstruction(); 
				double theValue = Double.parseDouble(drugDosage[0]);
				dosage.setDoseQuantity(null, theValue, drugDosage[1]);
				dosageList.add(dosage);
				med.setDosageInstruction(dosageList);
				//yyyymmdd
				DateTimeDt date = new DateTimeDt(lastFilled.substring(0,8));
				med.setDateWritten(date);
				med.addIdentifier("NDC", drugId);
				NarrativeStatusEnum narrative = null;
				med.getText().setStatus(narrative.GENERATED);
				StringBuffer buffer_narrative = new StringBuffer();
				buffer_narrative.append("<div>\n");
				buffer_narrative.append("<status value=\"generated\"/>\n");
				buffer_narrative.append("<div class=\"hapiHeaderText\">" + med.getMedication().getDisplay()+ "</div>\n");
				buffer_narrative.append("<table class=\"hapiPropertyTable\">\n");
				buffer_narrative.append("	<tbody>\n");
				buffer_narrative.append("		<tr>\n");
				buffer_narrative.append("			<td>Medication Name</td>\n");
				buffer_narrative.append("			<td>"+ med.getMedication().getDisplay() + "</td>\n");
				buffer_narrative.append("		</tr>\n");
				buffer_narrative.append("	</tbody>\n");
				buffer_narrative.append("</table>\n");
				buffer_narrative.append("</div>\n");
				//ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			   // String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(cond);
				String output = buffer_narrative.toString();
			    med.getText().setDiv(output);
			}			
		} catch (SQLException se) {
			se.printStackTrace();
			}
    	return med;
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
				//FhirContext ctx = new FhirContext();	
				obs.setId(Ids[0] + "-"+count+"-"+ Ids[2]);
				obs.setName(new CodeableConceptDt("http://loinc.org",obsConceptId)); 
				StringDt val = new StringDt(obsVal);
				obs.setValue(val);
			    obs.setComments(obsDate);
			    Date date = new Date();
				try {
					date = formatter.parse(obsDate);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String obsType = null;
				if (obsConceptId.equals("3141-9")){
					obsType = "Body Weight";
				}
				else if (obsConceptId.equals("8302-2")){
					obsType = "Body Height";
				}
				else if (obsConceptId.equals("9279-1")){
					obsType = "Respiration Rate";
				}
				else if (obsConceptId.equals("8867-4")){
					obsType = "Heart Beat";
				}
				else if (obsConceptId.equals("8480-6")){
					obsType = "Systolic BP";
				}
				else if (obsConceptId.equals("8462-4")){
					obsType = "Diastolic BP";
				}
				else if (obsConceptId.equals("8310-5")){
					obsType = "Body Temperature";
				}
				obs.setIssuedWithMillisPrecision(date);
				obs.setStatus(ObservationStatusEnum.FINAL);
				obs.setReliability(ObservationReliabilityEnum.OK);
				NarrativeStatusEnum narrative = null;
				obs.getText().setStatus(narrative.GENERATED);
				StringBuffer buffer_narrative = new StringBuffer();
				buffer_narrative.append("<div>\n");
				buffer_narrative.append("<div class=\"hapiHeaderText\">" + obsType + "</div>\n");
				buffer_narrative.append("<table class=\"hapiPropertyTable\">\n");
				buffer_narrative.append("	<tbody>\n");
				buffer_narrative.append("		<tr>\n");
				buffer_narrative.append("			<td>Value</td>\n");
				buffer_narrative.append("			<td>"+ val + "</td>\n");
				buffer_narrative.append("		</tr>\n");
				buffer_narrative.append("		<tr>\n");
				buffer_narrative.append("			<td>Date</td>\n");
				buffer_narrative.append("			<td>"+ obsDate + "</td>\n");
				buffer_narrative.append("		</tr>\n");
				buffer_narrative.append("	</tbody>\n");
				buffer_narrative.append("</table>\n");
				buffer_narrative.append("</div>\n");
				String output = buffer_narrative.toString();
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
				//cond.setNotes(condDate);
				CodeableConceptDt value = new CodeableConceptDt();
				value.setText(condName);
				CodingDt code = new CodingDt();
				code.setCode(condConceptId);
				code.setSystem("http://snomed.info/sct");
				code.setDisplay(condName);
				List<CodingDt> theValue = new ArrayList<CodingDt>();
				theValue.add(code);
				value.setCoding(theValue);
				cond.setCode(value );
				Date date = new Date();
				//String finaldate = condDate.substring(0, 8);
				//System.out.println(finaldate);
				try {
					date = formatter2.parse(condDate.substring(0, 8));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				cond.setDateAssertedWithDayPrecision(date);
				StringBuffer buffer_narrative = new StringBuffer();		
				//cond.setCode(value);
				//cond.addIdentifier("ICD9", condConceptId);
				cond.setStatus(ConditionStatusEnum.CONFIRMED);
				NarrativeStatusEnum narrative = null;
				cond.getText().setStatus(narrative.GENERATED);		

				buffer_narrative.append("<div>\n");
				buffer_narrative.append("<div class=\"hapiHeaderText\">" + cond.getCode().getText()+ "</div>\n");
				buffer_narrative.append("<table class=\"hapiPropertyTable\">\n");
				buffer_narrative.append("	<tbody>\n");
				buffer_narrative.append("		<tr>\n");
				buffer_narrative.append("			<td>Status</td>\n");
				buffer_narrative.append("			<td>"+ cond.getStatus().getValue() + "</td>\n");
				buffer_narrative.append("		</tr>\n");
				buffer_narrative.append("		<tr>\n");
				buffer_narrative.append("			<td>Date</td>\n");
				buffer_narrative.append("			<td>"+ date + "</td>\n");
				buffer_narrative.append("		</tr>\n");
				buffer_narrative.append("	</tbody>\n");
				buffer_narrative.append("</table>\n");
				buffer_narrative.append("</div>\n");
				String output = buffer_narrative.toString();
			    cond.getText().setDiv(output);
			}			
		} catch (SQLException se) {
			se.printStackTrace();
			}

		return cond;
	}
	
	public ArrayList<Condition> getConditionsByType(String code) {
		ArrayList<Condition> retVal = new ArrayList<Condition>();
		Connection conn = null;
		Connection conn2 = null;
	    Statement stmt = null;   
	    Statement stmt2 = null; 
	    Context context = null;
		DataSource datasource = null;
		Context context2 = null;
		DataSource datasource2 = null;
	    
	    String condConceptId = null;
	    String condId = null;
	    String condDate = null;
	    String condName = null;
	    String personId = null;
	    String id = null;
	    int count = 0;
	    try {
			
			context = new InitialContext();
			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/OMOP");
			conn = datasource.getConnection();
			stmt = conn.createStatement();
			context2 = new InitialContext();
			datasource2 = (DataSource) context2.lookup("java:/comp/env/jdbc/HealthPort");
			conn2 = datasource2.getConnection();
			stmt2 = conn2.createStatement();
			
			String sql = "SELECT condition_occurrence_id, person_id,condition_start_date, condition_id, condition_name FROM condition_occurrence WHERE condition_id= " + code;
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()){
				//condVal = rs.getString("observation_value");
				condConceptId = rs.getString("condition_id");
				condId = rs.getString("condition_occurrence_id");
				condDate = rs.getString("condition_start_date");
				condName = rs.getString("condition_name");
				personId = rs.getString("person_id");
				sql = "SELECT id FROM USER WHERE PERSONID= " + personId;
				ResultSet temprs = stmt2.executeQuery(sql);
				while(temprs.next()){
					id = temprs.getString("id");
					System.out.println(id);
				
					Condition cond = new Condition();
					cond.setId(id+"-"+count+"-"+condId);
					ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+id);
					cond.setSubject(subj);
					CodeableConceptDt value = new CodeableConceptDt();
					value.setText(condName);
					CodingDt codeString = new CodingDt();
					codeString.setCode(condConceptId);
					codeString.setSystem("http://snomed.info/sct");
					codeString.setDisplay(condName);
					List<CodingDt> theValue = new ArrayList<CodingDt>();
					theValue.add(codeString);
					value.setCoding(theValue);
					cond.setCode(value );
					Date date = new Date();
					//String finaldate = condDate.substring(0, 8);
					//System.out.println(finaldate);
					try {
						date = formatter2.parse(condDate.substring(0, 8));
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					cond.setDateAssertedWithDayPrecision(date);
					cond.setStatus(ConditionStatusEnum.CONFIRMED);
					NarrativeStatusEnum narrative = null;
					cond.getText().setStatus(narrative.GENERATED);
					StringBuffer buffer_narrative = new StringBuffer();		
					//cond.setCode(value);
					//cond.addIdentifier("ICD9", condConceptId);

					buffer_narrative.append("<div>\n");
					buffer_narrative.append("<div class=\"hapiHeaderText\">" + cond.getCode().getText()+ "</div>\n");
					buffer_narrative.append("<table class=\"hapiPropertyTable\">\n");
					buffer_narrative.append("	<tbody>\n");
					buffer_narrative.append("		<tr>\n");
					buffer_narrative.append("			<td>Status</td>\n");
					buffer_narrative.append("			<td>"+ cond.getStatus().getValue() + "</td>\n");
					buffer_narrative.append("		</tr>\n");
					buffer_narrative.append("		<tr>\n");
					buffer_narrative.append("			<td>Date</td>\n");
					buffer_narrative.append("			<td>"+ date + "</td>\n");
					buffer_narrative.append("		</tr>\n");
					buffer_narrative.append("	</tbody>\n");
					buffer_narrative.append("</table>\n");
					buffer_narrative.append("</div>\n");
					String output = buffer_narrative.toString();
				    cond.getText().setDiv(output);
				    retVal.add(cond);
				}
			}
		} catch (SQLException | NamingException se) {
			// TODO Auto-generated catch block
			se.printStackTrace();
			}
		
		return retVal;
		
	}
	
	public ArrayList<Observation> getObservationsByType(String name) {
		System.out.println("here");
		// TODO Auto-generated method stub
		ArrayList<Observation> retVal = new ArrayList<Observation>();
    	//ArrayList<String> retList = new ArrayList<String>();    	
    	//Get all Observations
		Connection conn = null;
		Connection conn2 = null;
	    Statement stmt = null;   
	    Statement stmt2 = null; 
	    Context context = null;
		DataSource datasource = null;
		Context context2 = null;
		DataSource datasource2 = null;  
	    String obsVal = null;
	    String obsConceptId = null;
	    String obsId = null;
	    String obsDate = null;
	    String personId=null;
	    String id = null;
	    int count = 0;
	    try {
	    	context = new InitialContext();
			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/OMOP");
			conn = datasource.getConnection();
			stmt = conn.createStatement();
			context2 = new InitialContext();
			datasource2 = (DataSource) context2.lookup("java:/comp/env/jdbc/HealthPort");
			conn2 = datasource2.getConnection();
			stmt2 = conn2.createStatement();
			
			String sql = "SELECT observation_id, person_id, observation_value, observation_date, observation_concept_id FROM observation WHERE observation_concept_id= " + "\'"+ name+"\'";
			ResultSet rs = stmt.executeQuery(sql);
			System.out.println(sql);
			
			while(rs.next()){
				obsVal = rs.getString("observation_value");
				obsConceptId = rs.getString("observation_concept_id");
				obsId = rs.getString("observation_id");
				obsDate = rs.getString("observation_date");
				personId = rs.getString("person_id");
				
				sql = "SELECT id FROM USER WHERE PERSONID= " + personId;
				ResultSet temprs = stmt2.executeQuery(sql);
				while(temprs.next()){
					id = temprs.getString("id");
					Observation obs = new Observation();
					obs.setId(id + "-"+count+"-"+ obsId);
					obs.setName(new CodeableConceptDt("http://loinc.org",obsConceptId)); 
					StringDt val = new StringDt(obsVal);
					obs.setValue(val);
					//obs.setComments("Body Weight");// if required, do -> if(Id[2] ==""){ set as ""} else{}
					obs.setComments(obsDate);
					ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+id);
					obs.setSubject(subj);
					Date date = new Date();
					try {
						date = formatter.parse(obsDate);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					obs.setIssuedWithMillisPrecision(date);
					obs.setStatus(ObservationStatusEnum.FINAL);
					obs.setReliability(ObservationReliabilityEnum.OK);
					NarrativeStatusEnum narrative = null;
					obs.getText().setStatus(narrative.GENERATED);

					String obsType = null;
					if (obsConceptId.equals("3141-9")){
						obsType = "Body Weight";
					}
					else if (obsConceptId.equals("8302-2")){
						obsType = "Body Height";
					}
					else if (obsConceptId.equals("9279-1")){
						obsType = "Respiration Rate";
					}
					else if (obsConceptId.equals("8867-4")){
						obsType = "Heart Beat";
					}
					else if (obsConceptId.equals("8480-6")){
						obsType = "Systolic BP";
					}
					else if (obsConceptId.equals("8462-4")){
						obsType = "Diastolic BP";
					}
					else if (obsConceptId.equals("8310-5")){
						obsType = "Body Temperature";
					}
					StringBuffer buffer_narrative = new StringBuffer();
					buffer_narrative.append("<div>\n");
					buffer_narrative.append("<div class=\"hapiHeaderText\">" + obsType + "</div>\n");
					buffer_narrative.append("<table class=\"hapiPropertyTable\">\n");
					buffer_narrative.append("	<tbody>\n");
					buffer_narrative.append("		<tr>\n");
					buffer_narrative.append("			<td>Value</td>\n");
					buffer_narrative.append("			<td>"+ val + "</td>\n");
					buffer_narrative.append("		</tr>\n");
					buffer_narrative.append("		<tr>\n");
					buffer_narrative.append("			<td>Date</td>\n");
					buffer_narrative.append("			<td>"+ obsDate + "</td>\n");
					buffer_narrative.append("		</tr>\n");
					buffer_narrative.append("	</tbody>\n");
					buffer_narrative.append("</table>\n");
					buffer_narrative.append("</div>\n");
					String output = buffer_narrative.toString();
				    obs.getText().setDiv(output);
				//ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			    //String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
			    //obs.getText().setDiv(output);
				//finalRetVal.add(obs);
				//return obs;
				retVal.add(obs);
					obs.getText().setDiv(output);
					//ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
				    //String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
				    //obs.getText().setDiv(output);
					//finalRetVal.add(obs);
					//return obs;
					retVal.add(obs);
				}
			}
		} catch (SQLException | NamingException se) {
			// TODO Auto-generated catch block
			se.printStackTrace();
			}
		return retVal;
	}
	
	public ArrayList<MedicationPrescription> getMedicationPrescriptionsByType(String medName) {
		ArrayList<MedicationPrescription> retVal = new ArrayList<MedicationPrescription>();
		Connection conn = null;
		Connection conn2 = null;
	    Statement stmt = null;   
	    Statement stmt2 = null; 
	    Context context = null;
		DataSource datasource = null;
		Context context2 = null;
		DataSource datasource2 = null;  
	    String[] drugDosage = null;
	    String drugName = null;
	    String lastFilled = null;
	    String drugId = null;
	    String drugConceptId = null;
	    String personId = null;
	    String id = null;
	    int count = 0;
	    try {
	    	context = new InitialContext();
			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/OMOP");
			conn = datasource.getConnection();
			stmt = conn.createStatement();
			context2 = new InitialContext();
			datasource2 = (DataSource) context2.lookup("java:/comp/env/jdbc/HealthPort");
			conn2 = datasource2.getConnection();
			stmt2 = conn2.createStatement();
			
			String sql = "SELECT drug_exposure_id,person_id, drug_id, drug_name, drug_dosage, last_filled_date FROM drug_exposure WHERE drug_name= " + "\'"+ medName+"\'";
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()){
				drugDosage = rs.getString("drug_dosage").split("\\s", 2);  
				drugId = rs.getString("drug_exposure_id");
				drugConceptId = rs.getString("drug_id");
				drugName = rs.getString("drug_name");
				lastFilled = rs.getString("last_filled_date");
				personId = rs.getString("person_id");
				
				sql = "SELECT id FROM USER WHERE PERSONID= " + personId;
				ResultSet temprs = stmt2.executeQuery(sql);
				while(temprs.next()){
					id = temprs.getString("id");
					
					MedicationPrescription med = new MedicationPrescription();
					med.setId(id+"-"+count+"-"+drugId); // This is object resource ID. 
					ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+id);
					med.setPatient(subj);
					ResourceReferenceDt medicationName = new ResourceReferenceDt();
					medicationName.setDisplay(drugName);
				
					med.setMedication(medicationName);
					ArrayList<MedicationPrescription.DosageInstruction> dosageList = new ArrayList<MedicationPrescription.DosageInstruction>();
					MedicationPrescription.DosageInstruction dosage = new MedicationPrescription.DosageInstruction(); 
					double theValue = Double.parseDouble(drugDosage[0]);
					dosage.setDoseQuantity(null, theValue, drugDosage[1]);
					dosageList.add(dosage);
					med.setDosageInstruction(dosageList);
					//yyyymmdd
					DateTimeDt date = new DateTimeDt(lastFilled.substring(0,8));
					med.setDateWritten(date);
					med.addIdentifier("NDC", drugConceptId);
					NarrativeStatusEnum narrative = null;
					med.getText().setStatus(narrative.GENERATED);
					StringBuffer buffer_narrative = new StringBuffer();
					buffer_narrative.append("<div>\n");
					buffer_narrative.append("<status value=\"generated\"/>\n");
					buffer_narrative.append("<div class=\"hapiHeaderText\">" + med.getMedication().getDisplay()+ "</div>\n");
					buffer_narrative.append("<table class=\"hapiPropertyTable\">\n");
					buffer_narrative.append("	<tbody>\n");
					buffer_narrative.append("		<tr>\n");
					buffer_narrative.append("			<td>Medication Name</td>\n");
					buffer_narrative.append("			<td>"+ med.getMedication().getDisplay() + "</td>\n");
					buffer_narrative.append("		</tr>\n");
					buffer_narrative.append("	</tbody>\n");
					buffer_narrative.append("</table>\n");
					buffer_narrative.append("</div>\n");
					String output = buffer_narrative.toString();
					med.getText().setDiv(output);
					retVal.add(med);
				}
			}
		} catch (SQLException | NamingException se) {
			// TODO Auto-generated catch block
			se.printStackTrace();
			}
		return retVal;
	}

	
}