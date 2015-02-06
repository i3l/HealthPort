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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringEscapeUtils;
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
import ca.uhn.fhir.model.primitive.IdDt;
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
	
	// ResourceID Differentiators
	static String Height = "h";
	static String Weight = "w";
	static String SystolicBP = "s";
	static String DiastolicBP = "d";
	static String Pulse = "p";
	static String Respiration = "r";
	static String Temperature = "t";
	static String Lab_Results = "l";
	
	// LOINC Code Mapping for Vital Sign
	static String weightLOINC = "3141-9";
	static String heightLOINC = "8302-2";
	static String respirationLOINC = "9279-1";
	static String pulseLOINC = "8867-4";
	static String systolicBPLOINC = "8480-6";
	static String diastolicBPLOINC = "8462-4";
	static String temperatureLOINC = "8310-5";

	private Observation createObs(String theId, String nameUri, String nameCode, String nameDisp, String theValue, String theUnit, Date date, String desc) {
		theId = theId.trim();
		nameUri = nameUri.trim();
		nameCode = nameCode.trim();
		nameDisp = nameDisp.trim();
		theValue = theValue.trim();
		theUnit = theUnit.trim();
		desc = desc.trim();
		
		Observation obs = new Observation();
		obs.setId(new IdDt(theId));
		
		// Observation Name
		CodingDt nameCoding = new CodingDt(nameUri, nameCode);
		nameCoding.setDisplay(nameDisp);
		
		ArrayList<CodingDt> codingList = new ArrayList<CodingDt>();
		codingList.add(nameCoding);
		CodeableConceptDt nameDt = new CodeableConceptDt();
		nameDt.setCoding(codingList);

		obs.setName(nameDt);
		
		// Observation Value[x], x=Quantity, value=double
		if (!theValue.isEmpty()) {
			// Remove any commas
			theValue = theValue.replace(",", "");
			QuantityDt qDt = new QuantityDt (Double.parseDouble(theValue));
			if (!theUnit.isEmpty()) {
				qDt.setUnits(theUnit);
//				qDt.setSystem(vUri);  These are optional...
//				qDt.setCode(vCode);
			}
		
			obs.setValue(qDt);
		}
		// Observation Status
		obs.setStatus(ObservationStatusEnum.FINAL);
		
		// Reliability
		obs.setReliability(ObservationReliabilityEnum.OK);

		if (date != null) {
			obs.setApplies(new DateTimeDt(date));
		}
		
		// Human Readable Section
		obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
		String textBody = date.toString()+" "+nameDisp+"="+theValue+" "+theUnit+" "+desc;		
		textBody = StringEscapeUtils.escapeHtml4(textBody);
		//System.out.println(textBody);
	    obs.getText().setDiv(textBody);

		return obs;
	}
	
	
	public ArrayList<Observation> getObservations(HealthPortUserInfo userInfo) {
		// TODO Auto-generated method stub
		ArrayList<Observation> retVal = new ArrayList<Observation>();
    	//ArrayList<String> retList = new ArrayList<String>();    	

		//Get all Observations
	    Connection conn = null;
	    Statement stmt = null;  
		Context context = null;
		DataSource datasource;
		
	    try {
	    	context = new InitialContext();
			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = datasource.getConnection();
			
			stmt = conn.createStatement();
			
			// Get Vital Sign for this patient.
			String sql = "SELECT * FROM vital_sign WHERE Member_ID='"+userInfo.personId+"'";
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()){
				Date dateTime = new java.util.Date(rs.getDate("Encounter_Date").getTime());
				
				// Height.
				String height = rs.getString("Height");				
				if (!height.isEmpty()) {
					String heightUnit = rs.getString("Height_Units");
					Observation obs = createObs(userInfo.userId+"-"+Height+"-"+rs.getString("Encounter_ID"), 
							"http://loinc.org", 
							heightLOINC, 
							"Body Height", 
							height, 
							heightUnit,
							dateTime,
							"");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"+userInfo.userId));
					retVal.add(obs);
				}
				
				// Weight.
				String weight = rs.getString("Weight");
				if (!weight.isEmpty()) {
					String weightUnit = rs.getString("Weight_Units");
					Observation obs = createObs(userInfo.userId+"-"+Weight+"-"+rs.getString("Encounter_ID"), 
							"http://loinc.org", 
							weightLOINC, 
							"Body Weight", 
							weight, 
							weightUnit, 
							dateTime,
							"");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"+userInfo.userId));
					retVal.add(obs);					
				}
				
				// Respiration
				String respiration = rs.getString("Respiration");
				if (!respiration.isEmpty()) {
					Observation obs = createObs(userInfo.userId+"-"+Respiration+"-"+rs.getString("Encounter_ID"), 
							"http://loinc.org", 
							respirationLOINC, 
							"Respiration Rate", 
							respiration, 
							"", 
							dateTime,
							"");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"+userInfo.userId));
					retVal.add(obs);					
				}

				// Pulse
				String pulse = rs.getString("Pulse");
				if (!pulse.isEmpty()) {
					Observation obs = createObs(userInfo.userId+"-"+Pulse+"-"+rs.getString("Encounter_ID"), 
							"http://loinc.org", 
							pulseLOINC, 
							"Heart Beat", 
							pulse, 
							"",
							dateTime,
							"");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"+userInfo.userId));
					retVal.add(obs);					
				}
				
				// Systolic BP
				String systolicBP = rs.getString("SystolicBP");
				if (!systolicBP.isEmpty()) {
					Observation obs = createObs(userInfo.userId+"-"+SystolicBP+"-"+rs.getString("Encounter_ID"), 
							"http://loinc.org", 
							systolicBPLOINC, 
							"Systolic BP", 
							systolicBP, 
							"mm[Hg]",
							dateTime,
							"");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"+userInfo.userId));
					retVal.add(obs);					
				}
				
				// Diastolic BP
				String diastolicBP = rs.getString("DiastolicBP");
				if (!diastolicBP.isEmpty()) {
					Observation obs = createObs(userInfo.userId+"-"+DiastolicBP+"-"+rs.getString("Encounter_ID"), 
							"http://loinc.org", 
							diastolicBPLOINC, 
							"Diastolic BP", 
							diastolicBP, 
							"mm[Hg]",
							dateTime,
							"");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"+userInfo.userId));
					retVal.add(obs);					
				}
				
				// Temperature
				String temp = rs.getString("Temperature");
				if (!temp.isEmpty()) {
					String tempUnit = rs.getString("Temperature_Units");
					Observation obs = createObs(userInfo.userId+"-"+Temperature+"-"+rs.getString("Encounter_ID"), 
							"http://loinc.org", 
							temperatureLOINC, 
							"Body Temperature", 
							temp, 
							tempUnit,
							dateTime,
							"");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"+userInfo.userId));
					retVal.add(obs);					
				}
			}
			
			// Get Lab Result
			sql = "SELECT Lab_Result_ID, Result_Name, Result_Status, Result_LOINC, Result_Description, Numeric_Result, Units, Encounter_ID, Date_Resulted FROM lab_results WHERE Member_ID='"+userInfo.personId+"'";
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Date dateTime = new java.util.Date(rs.getDate("Date_Resulted").getTime());
				Observation obs = createObs(userInfo.userId+"-"+Lab_Results+"-"+rs.getInt("Lab_Result_ID"), 
						"http://loinc.org", 
						rs.getString("Result_LOINC"), 
						rs.getString("Result_Name"), 
						rs.getString("Numeric_Result"), 
						rs.getString("Units"),
						dateTime,
						rs.getString("Result_Description"));

				// Observation Reference to Patient
				obs.setSubject(new ResourceReferenceDt("Patient/"+userInfo.userId));
				retVal.add(obs);					
			}
			conn.close();
		} catch (SQLException se) {
			// TODO Auto-generated catch block
			se.printStackTrace();
		} catch (NamingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
		String[] Ids  = resourceId.split("\\-", 3);
    	//Ids[0] -> user id
		//Ids[1] -> (locally defined) observation type
		//Ids[2] -> member id (in exact database).
		
		// We know that we are getting observations. Use the
		// reference ID to figure out which observations we need to return
	    Connection conn = null;
	    Statement stmt = null;
	    //String personId = null;
	    String sql = null; 
	    ResultSet rs = null;
	    Observation obs = null;
	    
		Context context = null;
		DataSource datasource;

	    try {
	    	context = new InitialContext();
			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = datasource.getConnection();
			
			stmt = conn.createStatement();

//			URL = url + "/" + dbName;
//			conn = DriverManager.getConnection(URL, username, password);
//			sql = "SELECT observation_id, observation_value, observation_date, observation_concept_id FROM observation WHERE observation_id= " + Ids[2];
			if (Ids[1].equalsIgnoreCase(Lab_Results)) {
				// We are asked to return lab result part of observation. 
				// Use the member ID to search and return the observations
				sql = "SELECT Result_Name, Result_Status, Result_LOINC, Result_Description, Numeric_Result, Units, Encounter_ID, Date_Resulted FROM lab_results WHERE Lab_Result_ID="+Ids[2];
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					Date dateTime = new java.util.Date(rs.getDate("Date_Resulted").getTime());
					obs = createObs(Ids[0]+"-"+Ids[1]+"-"+rs.getString("Encounter_ID"), 
							"http://loinc.org", 
							rs.getString("Result_LOINC"), 
							rs.getString("Result_Name"), 
							rs.getString("Numeric_Result"), 
							rs.getString("Units"),
							dateTime,
							rs.getString("Result_Description"));

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"+Ids[0]));				
				}
			} else {
				sql = "SELECT * FROM vital_sign WHERE Encounter_ID='"+Ids[2]+"'";
				rs = stmt.executeQuery(sql);
				while(rs.next()){
					Date dateTime = new java.util.Date(rs.getDate("Encounter_Date").getTime());
					String Units="";
					String Value="";
					String DispName="";
					String ConceptCode="";
					if (Ids[1].equalsIgnoreCase(Weight)) {
						ConceptCode = weightLOINC;
						DispName = "Body Weight";
						Value = rs.getString("Weight");
						Units = rs.getString("Weight_Units");
					} else if (Ids[1].equalsIgnoreCase(Height)) {
						ConceptCode = heightLOINC;
						DispName = "Body Height";
						Value = rs.getString("Height");
						Units = rs.getString("Height_Units");
					} else if (Ids[1].equalsIgnoreCase(Respiration)) {
						ConceptCode = respirationLOINC;
						DispName = "Respiration Rate";
						Value = rs.getString("Respiration");
					} else if (Ids[1].equalsIgnoreCase(Pulse)) {
						ConceptCode = pulseLOINC;
						DispName = "Heart Beat";
						Value = rs.getString("Pulse");
					} else if (Ids[1].equalsIgnoreCase(SystolicBP)) {
						ConceptCode = systolicBPLOINC;
						DispName = "Systolic BP";
						Value = rs.getString("SystoliBP");
						Units = "mm[Hg]";
					} else if (Ids[1].equalsIgnoreCase(DiastolicBP))  {
						ConceptCode = diastolicBPLOINC;
						DispName = "Diastolic BP";
						Value = rs.getString("DiastolicBP");
						Units = "mm[Hg]";
					} else if (Ids[1].equalsIgnoreCase(Temperature)) {
						ConceptCode = temperatureLOINC;
						DispName = "Body Temperature";
						Value = rs.getString("Temperature");
						Units = rs.getString("Temperature_Units");
					} 
					
					if (!Value.isEmpty()) {
						obs = createObs(Ids[0]+"-"+Ids[1]+"-"+Ids[2], 
								"http://loinc.org", 
								ConceptCode, 
								DispName, 
								Value, 
								Units,
								dateTime,
								"");

						// Observation Reference to Patient
						obs.setSubject(new ResourceReferenceDt("Patient/"+Ids[0]));
					}
				}
			}
			
			conn.close();
		} catch (SQLException se) {
			se.printStackTrace();
			} catch (NamingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
	
	public ArrayList<Observation> getObservationsByType(String systemName, String codeName) {
		// In Observation, we search vital and lab result data. 
		// 
		
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
			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = datasource.getConnection();
			stmt = conn.createStatement();
			context2 = new InitialContext();
			datasource2 = (DataSource) context2.lookup("java:/comp/env/jdbc/HealthPort");
			conn2 = datasource2.getConnection();
			stmt2 = conn2.createStatement();
			
			if (codeName.equalsIgnoreCase(weightLOINC) ||
					codeName.equalsIgnoreCase(heightLOINC) ||
					codeName.equalsIgnoreCase(respirationLOINC) ||
					codeName.equalsIgnoreCase(pulseLOINC) ||
					codeName.equalsIgnoreCase(systolicBPLOINC) ||
					codeName.equalsIgnoreCase(diastolicBPLOINC) ||
					codeName.equalsIgnoreCase(temperatureLOINC)) {
				String sql = "SELECT * FROM vital_sign";
				ResultSet rs = stmt.executeQuery(sql);
				while(rs.next()){
					String memberID = rs.getString("Member_ID");
					String memSql = "SELECT ID FROM USER WHERE ORGANIZATIONID=3 AND PERSONID='"+memberID+"'";
					ResultSet rs2 = stmt2.executeQuery(memSql);
					String hpUserID = ""; 
					while (rs2.next()) {
						hpUserID = rs2.getString("ID");
					}
					if (hpUserID.isEmpty()) {
						// This is in fact an error since we need to have all members in Synthetic DB
						// in HealthPort DB. We just skip..
						System.out.println("[SyntheticEHRPort:getObservationsByType] Failed to get this user,"+memberID+", in HealthPort DB");
						continue;  
					}
					
					Date dateTime = new java.util.Date(rs.getDate("Encounter_Date").getTime());
					if (codeName.equalsIgnoreCase(heightLOINC)) {
						String height = rs.getString("Height");				
						if (!height.isEmpty()) {
							String heightUnit = rs.getString("Height_Units");
							Observation obs = createObs(hpUserID+"-"+Height+"-"+rs.getString("Encounter_ID"), 
									"http://loinc.org", 
									heightLOINC, 
									"Body Height", 
									height, 
									heightUnit,
									dateTime,
									"");
	
							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"+hpUserID));
							retVal.add(obs);
						}
					} else if (codeName.equalsIgnoreCase(weightLOINC)) {
						String weight = rs.getString("Weight");
						if (!weight.isEmpty()) {
							String weightUnit = rs.getString("Weight_Units");
							Observation obs = createObs(hpUserID+"-"+Weight+"-"+rs.getString("Encounter_ID"), 
									"http://loinc.org", 
									weightLOINC, 
									"Body Weight", 
									weight, 
									weightUnit, 
									dateTime,
									"");
	
							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"+hpUserID));
							retVal.add(obs);					
						}
					} else if (codeName.equalsIgnoreCase(respirationLOINC)) {
						String respiration = rs.getString("Respiration");
						if (!respiration.isEmpty()) {
							Observation obs = createObs(hpUserID+"-"+Respiration+"-"+rs.getString("Encounter_ID"), 
									"http://loinc.org", 
									respirationLOINC, 
									"Respiration Rate", 
									respiration, 
									"", 
									dateTime,
									"");
	
							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"+hpUserID));
							retVal.add(obs);					
						}
					} else if (codeName.equalsIgnoreCase(pulseLOINC)) {
						String pulse = rs.getString("Pulse");
						if (!pulse.isEmpty()) {
							Observation obs = createObs(hpUserID+"-"+Pulse+"-"+rs.getString("Encounter_ID"), 
									"http://loinc.org", 
									pulseLOINC, 
									"Heart Beat", 
									pulse, 
									"",
									dateTime,
									"");
	
							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"+hpUserID));
							retVal.add(obs);					
						}					
					} else if (codeName.equalsIgnoreCase(systolicBPLOINC)) {
						String systolicBP = rs.getString("SystolicBP");
						if (!systolicBP.isEmpty()) {
							Observation obs = createObs(hpUserID+"-"+SystolicBP+"-"+rs.getString("Encounter_ID"), 
									"http://loinc.org", 
									systolicBPLOINC, 
									"Systolic BP", 
									systolicBP, 
									"mm[Hg]",
									dateTime,
									"");
	
							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"+hpUserID));
							retVal.add(obs);					
						}
					} else if (codeName.equalsIgnoreCase(diastolicBPLOINC)) {
						String diastolicBP = rs.getString("DiastolicBP");
						if (!diastolicBP.isEmpty()) {
							Observation obs = createObs(hpUserID+"-"+DiastolicBP+"-"+rs.getString("Encounter_ID"), 
									"http://loinc.org", 
									diastolicBPLOINC, 
									"Diastolic BP", 
									diastolicBP, 
									"mm[Hg]",
									dateTime,
									"");
	
							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"+hpUserID));
							retVal.add(obs);					
						}					
					} else if (codeName.equalsIgnoreCase(temperatureLOINC)) {
						String temp = rs.getString("Temperature");
						if (!temp.isEmpty()) {
							String tempUnit = rs.getString("Temperature_Units");
							Observation obs = createObs(hpUserID+"-"+Temperature+"-"+rs.getString("Encounter_ID"), 
									"http://loinc.org", 
									temperatureLOINC, 
									"Body Temperature", 
									temp, 
									tempUnit,
									dateTime,
									"");
	
							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"+hpUserID));
							retVal.add(obs);					
						}					
					} 
				}
			} else {
				// No Vital Sign.Check Lab Result.
				// Get Lab Result
				String sql = "SELECT Lab_Result_ID, Member_ID, Result_Name, Result_Status, Result_LOINC, Result_Description, Numeric_Result, Units, Encounter_ID, Date_Resulted FROM lab_results WHERE Result_LOINC='"+codeName+"'";
				ResultSet rs = stmt.executeQuery(sql);
				while (rs.next()) {
					String memberID = rs.getString("Member_ID");
					String memSql = "SELECT ID FROM USER WHERE ORGANIZATIONID=3 AND PERSONID='"+memberID+"'";
					ResultSet rs2 = stmt2.executeQuery(memSql);
					String hpUserID = ""; 
					while (rs2.next()) {
						hpUserID = rs2.getString("ID");
					}
					if (hpUserID.isEmpty()) {
						// This is in fact an error since we need to have all members in Synthetic DB
						// in HealthPort DB. We just skip..
						System.out.println("[SyntheticEHRPort:getObservationsByType] Failed to get this user,"+memberID+", in HealthPort DB");
						continue;  
					}

					Date dateTime = new java.util.Date(rs.getDate("Date_Resulted").getTime());
					Observation obs = createObs(hpUserID+"-"+Lab_Results+"-"+rs.getInt("Lab_Result_ID"), 
							"http://loinc.org", 
							rs.getString("Result_LOINC"), 
							rs.getString("Result_Name"), 
							rs.getString("Numeric_Result"), 
							rs.getString("Units"),
							dateTime,
							rs.getString("Result_Description"));

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"+hpUserID));
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