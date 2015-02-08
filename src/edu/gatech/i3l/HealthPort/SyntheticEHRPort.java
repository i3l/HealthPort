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
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu.composite.CodingDt;
import ca.uhn.fhir.model.dstu.composite.ContainedDt;
import ca.uhn.fhir.model.dstu.composite.QuantityDt;
import ca.uhn.fhir.model.dstu.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.Medication;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription.Dispense;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription.DosageInstruction;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.valueset.ConditionStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.MedicationPrescriptionStatusEnum;
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

	private Observation createObs(String theId, String nameUri,
			String nameCode, String nameDisp, String theValue, String theUnit,
			Date date, String desc) {
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
			QuantityDt qDt = new QuantityDt(Double.parseDouble(theValue));
			if (!theUnit.isEmpty()) {
				qDt.setUnits(theUnit);
				// qDt.setSystem(vUri); These are optional...
				// qDt.setCode(vCode);
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
		String textBody = date.toString() + " " + nameDisp + "=" + theValue
				+ " " + theUnit + " " + desc;
		textBody = StringEscapeUtils.escapeHtml4(textBody);
		// System.out.println(textBody);
		obs.getText().setDiv(textBody);

		return obs;
	}

	public ArrayList<Observation> getObservations(HealthPortUserInfo userInfo) {
		// TODO Auto-generated method stub
		ArrayList<Observation> retVal = new ArrayList<Observation>();
		// ArrayList<String> retList = new ArrayList<String>();

		// Get all Observations
		Connection conn = null;
		Statement stmt = null;
		Context context = null;
		DataSource datasource;

		try {
			context = new InitialContext();
			datasource = (DataSource) context
					.lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = datasource.getConnection();

			stmt = conn.createStatement();

			// Get Vital Sign for this patient.
			String sql = "SELECT * FROM vital_sign WHERE Member_ID='"
					+ userInfo.personId + "'";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Date dateTime = new java.util.Date(rs.getDate("Encounter_Date")
						.getTime());

				// Height.
				String height = rs.getString("Height");
				if (!height.isEmpty()) {
					String heightUnit = rs.getString("Height_Units");
					Observation obs = createObs(userInfo.userId + "-" + Height
							+ "-" + rs.getString("Encounter_ID"),
							"http://loinc.org", heightLOINC, "Body Height",
							height, heightUnit, dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"
							+ userInfo.userId));
					retVal.add(obs);
				}

				// Weight.
				String weight = rs.getString("Weight");
				if (!weight.isEmpty()) {
					String weightUnit = rs.getString("Weight_Units");
					Observation obs = createObs(userInfo.userId + "-" + Weight
							+ "-" + rs.getString("Encounter_ID"),
							"http://loinc.org", weightLOINC, "Body Weight",
							weight, weightUnit, dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"
							+ userInfo.userId));
					retVal.add(obs);
				}

				// Respiration
				String respiration = rs.getString("Respiration");
				if (!respiration.isEmpty()) {
					Observation obs = createObs(userInfo.userId + "-"
							+ Respiration + "-" + rs.getString("Encounter_ID"),
							"http://loinc.org", respirationLOINC,
							"Respiration Rate", respiration, "", dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"
							+ userInfo.userId));
					retVal.add(obs);
				}

				// Pulse
				String pulse = rs.getString("Pulse");
				if (!pulse.isEmpty()) {
					Observation obs = createObs(userInfo.userId + "-" + Pulse
							+ "-" + rs.getString("Encounter_ID"),
							"http://loinc.org", pulseLOINC, "Heart Beat",
							pulse, "", dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"
							+ userInfo.userId));
					retVal.add(obs);
				}

				// Systolic BP
				String systolicBP = rs.getString("SystolicBP");
				if (!systolicBP.isEmpty()) {
					Observation obs = createObs(userInfo.userId + "-"
							+ SystolicBP + "-" + rs.getString("Encounter_ID"),
							"http://loinc.org", systolicBPLOINC, "Systolic BP",
							systolicBP, "mm[Hg]", dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"
							+ userInfo.userId));
					retVal.add(obs);
				}

				// Diastolic BP
				String diastolicBP = rs.getString("DiastolicBP");
				if (!diastolicBP.isEmpty()) {
					Observation obs = createObs(userInfo.userId + "-"
							+ DiastolicBP + "-" + rs.getString("Encounter_ID"),
							"http://loinc.org", diastolicBPLOINC,
							"Diastolic BP", diastolicBP, "mm[Hg]", dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"
							+ userInfo.userId));
					retVal.add(obs);
				}

				// Temperature
				String temp = rs.getString("Temperature");
				if (!temp.isEmpty()) {
					String tempUnit = rs.getString("Temperature_Units");
					Observation obs = createObs(userInfo.userId + "-"
							+ Temperature + "-" + rs.getString("Encounter_ID"),
							"http://loinc.org", temperatureLOINC,
							"Body Temperature", temp, tempUnit, dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"
							+ userInfo.userId));
					retVal.add(obs);
				}
			}

			// Get Lab Result
			sql = "SELECT Lab_Result_ID, Result_Name, Result_Status, Result_LOINC, Result_Description, Numeric_Result, Units, Encounter_ID, Date_Resulted FROM lab_results WHERE Member_ID='"
					+ userInfo.personId + "'";
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Date dateTime = new java.util.Date(rs.getDate("Date_Resulted")
						.getTime());
				Observation obs = createObs(userInfo.userId + "-" + Lab_Results
						+ "-" + rs.getInt("Lab_Result_ID"), "http://loinc.org",
						rs.getString("Result_LOINC"),
						rs.getString("Result_Name"),
						rs.getString("Numeric_Result"), rs.getString("Units"),
						dateTime, rs.getString("Result_Description"));

				// Observation Reference to Patient
				obs.setSubject(new ResourceReferenceDt("Patient/"
						+ userInfo.userId));
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

	public Observation getObservation(String resourceId) {
		// ArrayList<String> obsList = new ArrayList<String>();
		String[] Ids = resourceId.split("\\-", 3);
		// Ids[0] -> user id
		// Ids[1] -> (locally defined) observation type
		// Ids[2] -> member id (in exact database).

		if (Ids.length != 3)
			return null;

		// We know that we are getting observations. Use the
		// reference ID to figure out which observations we need to return
		Connection conn = null;
		Statement stmt = null;
		// String personId = null;
		String sql = null;
		ResultSet rs = null;
		Observation obs = null;

		Context context = null;
		DataSource datasource;

		try {
			context = new InitialContext();
			datasource = (DataSource) context
					.lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = datasource.getConnection();

			stmt = conn.createStatement();

			// URL = url + "/" + dbName;
			// conn = DriverManager.getConnection(URL, username, password);
			// sql =
			// "SELECT observation_id, observation_value, observation_date, observation_concept_id FROM observation WHERE observation_id= "
			// + Ids[2];
			if (Ids[1].equalsIgnoreCase(Lab_Results)) {
				// We are asked to return lab result part of observation.
				// Use the member ID to search and return the observations
				sql = "SELECT Result_Name, Result_Status, Result_LOINC, Result_Description, Numeric_Result, Units, Encounter_ID, Date_Resulted FROM lab_results WHERE Lab_Result_ID="
						+ Ids[2];
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					Date dateTime = new java.util.Date(rs.getDate(
							"Date_Resulted").getTime());
					obs = createObs(
							Ids[0] + "-" + Ids[1] + "-"
									+ rs.getString("Encounter_ID"),
							"http://loinc.org", rs.getString("Result_LOINC"),
							rs.getString("Result_Name"),
							rs.getString("Numeric_Result"),
							rs.getString("Units"), dateTime,
							rs.getString("Result_Description"));

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/" + Ids[0]));
				}
			} else {
				sql = "SELECT * FROM vital_sign WHERE Encounter_ID='" + Ids[2]
						+ "'";
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					Date dateTime = new java.util.Date(rs.getDate(
							"Encounter_Date").getTime());
					String Units = "";
					String Value = "";
					String DispName = "";
					String ConceptCode = "";
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
					} else if (Ids[1].equalsIgnoreCase(DiastolicBP)) {
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
						obs = createObs(Ids[0] + "-" + Ids[1] + "-" + Ids[2],
								"http://loinc.org", ConceptCode, DispName,
								Value, Units, dateTime, "");

						// Observation Reference to Patient
						obs.setSubject(new ResourceReferenceDt("Patient/"
								+ Ids[0]));
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

	public ArrayList<Observation> getObservationsByCodeSystem(
			String codeSystem, String code) {
		// In Observation, we search vital and lab result data.
		//

		ArrayList<Observation> retVal = new ArrayList<Observation>();

		// Check if the system is LONIC. If codeSystem is not set, we assume
		// it's LOINC
		if (codeSystem != null
				&& !codeSystem.isEmpty()
				&& !codeSystem.equalsIgnoreCase("http://loinc.org")
				&& !codeSystem
						.equalsIgnoreCase("urn:oid:2.16.840.1.113883.6.1")) {
			// SyntheticEHR only has LOINC code name for observation data. But,
			// pass system name anyway just in case.
			return retVal;
		}

		// ArrayList<String> retList = new ArrayList<String>();
		// Get all Observations
		Connection conn = null;
		Connection conn2 = null;
		Statement stmt = null;
		Statement stmt2 = null;
		Context context = null;
		DataSource datasource = null;
		Context context2 = null;
		DataSource datasource2 = null;
		try {
			context = new InitialContext();
			datasource = (DataSource) context
					.lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = datasource.getConnection();
			stmt = conn.createStatement();
			context2 = new InitialContext();
			datasource2 = (DataSource) context2
					.lookup("java:/comp/env/jdbc/HealthPort");
			conn2 = datasource2.getConnection();
			stmt2 = conn2.createStatement();

			if (code.equalsIgnoreCase(weightLOINC)
					|| code.equalsIgnoreCase(heightLOINC)
					|| code.equalsIgnoreCase(respirationLOINC)
					|| code.equalsIgnoreCase(pulseLOINC)
					|| code.equalsIgnoreCase(systolicBPLOINC)
					|| code.equalsIgnoreCase(diastolicBPLOINC)
					|| code.equalsIgnoreCase(temperatureLOINC)) {
				String sql = "SELECT * FROM vital_sign";
				ResultSet rs = stmt.executeQuery(sql);
				while (rs.next()) {
					String memberID = rs.getString("Member_ID");
					String memSql = "SELECT ID FROM USER WHERE ORGANIZATIONID=3 AND PERSONID='"
							+ memberID + "'";
					ResultSet rs2 = stmt2.executeQuery(memSql);
					String hpUserID = "";
					while (rs2.next()) {
						hpUserID = rs2.getString("ID");
					}
					if (hpUserID.isEmpty()) {
						// This is in fact an error since we need to have all
						// members in Synthetic DB
						// in HealthPort DB. We just skip..
						System.out
								.println("[SyntheticEHRPort:getObservationsByType] Failed to get this user,"
										+ memberID + ", in HealthPort DB");
						continue;
					}

					Date dateTime = new java.util.Date(rs.getDate(
							"Encounter_Date").getTime());
					if (code.equalsIgnoreCase(heightLOINC)) {
						String height = rs.getString("Height");
						if (!height.isEmpty()) {
							String heightUnit = rs.getString("Height_Units");
							Observation obs = createObs(hpUserID + "-" + Height
									+ "-" + rs.getString("Encounter_ID"),
									"http://loinc.org", heightLOINC,
									"Body Height", height, heightUnit,
									dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ hpUserID));
							retVal.add(obs);
						}
					} else if (code.equalsIgnoreCase(weightLOINC)) {
						String weight = rs.getString("Weight");
						if (!weight.isEmpty()) {
							String weightUnit = rs.getString("Weight_Units");
							Observation obs = createObs(hpUserID + "-" + Weight
									+ "-" + rs.getString("Encounter_ID"),
									"http://loinc.org", weightLOINC,
									"Body Weight", weight, weightUnit,
									dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ hpUserID));
							retVal.add(obs);
						}
					} else if (code.equalsIgnoreCase(respirationLOINC)) {
						String respiration = rs.getString("Respiration");
						if (!respiration.isEmpty()) {
							Observation obs = createObs(
									hpUserID + "-" + Respiration + "-"
											+ rs.getString("Encounter_ID"),
									"http://loinc.org", respirationLOINC,
									"Respiration Rate", respiration, "",
									dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ hpUserID));
							retVal.add(obs);
						}
					} else if (code.equalsIgnoreCase(pulseLOINC)) {
						String pulse = rs.getString("Pulse");
						if (!pulse.isEmpty()) {
							Observation obs = createObs(hpUserID + "-" + Pulse
									+ "-" + rs.getString("Encounter_ID"),
									"http://loinc.org", pulseLOINC,
									"Heart Beat", pulse, "", dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ hpUserID));
							retVal.add(obs);
						}
					} else if (code.equalsIgnoreCase(systolicBPLOINC)) {
						String systolicBP = rs.getString("SystolicBP");
						if (!systolicBP.isEmpty()) {
							Observation obs = createObs(
									hpUserID + "-" + SystolicBP + "-"
											+ rs.getString("Encounter_ID"),
									"http://loinc.org", systolicBPLOINC,
									"Systolic BP", systolicBP, "mm[Hg]",
									dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ hpUserID));
							retVal.add(obs);
						}
					} else if (code.equalsIgnoreCase(diastolicBPLOINC)) {
						String diastolicBP = rs.getString("DiastolicBP");
						if (!diastolicBP.isEmpty()) {
							Observation obs = createObs(
									hpUserID + "-" + DiastolicBP + "-"
											+ rs.getString("Encounter_ID"),
									"http://loinc.org", diastolicBPLOINC,
									"Diastolic BP", diastolicBP, "mm[Hg]",
									dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ hpUserID));
							retVal.add(obs);
						}
					} else if (code.equalsIgnoreCase(temperatureLOINC)) {
						String temp = rs.getString("Temperature");
						if (!temp.isEmpty()) {
							String tempUnit = rs.getString("Temperature_Units");
							Observation obs = createObs(
									hpUserID + "-" + Temperature + "-"
											+ rs.getString("Encounter_ID"),
									"http://loinc.org", temperatureLOINC,
									"Body Temperature", temp, tempUnit,
									dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ hpUserID));
							retVal.add(obs);
						}
					}
				}
			} else {
				// No Vital Sign.Check Lab Result.
				// Get Lab Result
				String sql = "SELECT Lab_Result_ID, Member_ID, Result_Name, Result_Status, Result_LOINC, Result_Description, Numeric_Result, Units, Encounter_ID, Date_Resulted FROM lab_results WHERE Result_LOINC='"
						+ code + "'";
				ResultSet rs = stmt.executeQuery(sql);
				while (rs.next()) {
					String memberID = rs.getString("Member_ID");
					String memSql = "SELECT ID FROM USER WHERE ORGANIZATIONID=3 AND PERSONID='"
							+ memberID + "'";
					ResultSet rs2 = stmt2.executeQuery(memSql);
					String hpUserID = "";
					while (rs2.next()) {
						hpUserID = rs2.getString("ID");
					}
					if (hpUserID.isEmpty()) {
						// This is in fact an error since we need to have all
						// members in Synthetic DB
						// in HealthPort DB. We just skip..
						System.out
								.println("[SyntheticEHRPort:getObservationsByType] Failed to get this user,"
										+ memberID + ", in HealthPort DB");
						continue;
					}

					Date dateTime = new java.util.Date(rs.getDate(
							"Date_Resulted").getTime());
					Observation obs = createObs(hpUserID + "-" + Lab_Results
							+ "-" + rs.getInt("Lab_Result_ID"),
							"http://loinc.org", rs.getString("Result_LOINC"),
							rs.getString("Result_Name"),
							rs.getString("Numeric_Result"),
							rs.getString("Units"), dateTime,
							rs.getString("Result_Description"));

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/"
							+ hpUserID));
					retVal.add(obs);
				}
			}
		} catch (SQLException | NamingException se) {
			// TODO Auto-generated catch block
			se.printStackTrace();
		}
		return retVal;
	}

	private Condition createCondition(String ID, String subjRef,
			String nameUri, String nameCode, String nameDisp, Date dateTime) {
		Condition condition = new Condition();

		// Set ID
		condition.setId(ID);

		// Set subject reference to Patient
		ResourceReferenceDt subj = new ResourceReferenceDt(subjRef);
		condition.setSubject(subj);

		// Set Code
		nameDisp = StringEscapeUtils.escapeHtml4(nameDisp);
		CodingDt nameCoding = new CodingDt(nameUri, nameCode);
		nameCoding.setDisplay(nameDisp);
		ArrayList<CodingDt> codingList = new ArrayList<CodingDt>();
		codingList.add(nameCoding);
		CodeableConceptDt codeDt = new CodeableConceptDt();
		codeDt.setCoding(codingList);
		condition.setCode(codeDt);

		// Set status
		condition.setStatus(ConditionStatusEnum.CONFIRMED);

		// Optional onsetdate
		condition.setOnset(new DateDt(dateTime));

		// Optional human readable part.
		String textBody = "<table class=\"hapiPropertyTable\">";
		textBody = textBody
				+ "<tr><td>Problem</td><td>Onset Date</td><td>ICD-9</td></tr>";
		textBody = textBody + "<tr><td>" + nameDisp + "</td>" + "<td>"
				+ dateTime.toString() + "</td>" + "<td>" + nameCode
				+ "</td></tr></table>";
		condition.getText().setDiv(textBody);
		condition.getText().setStatus(NarrativeStatusEnum.GENERATED);

		return condition;
	}

	public ArrayList<Condition> getConditions(HealthPortUserInfo userInfo) {
		ArrayList<Condition> retVal = new ArrayList<Condition>();
		Condition condition = null;

		Context context;
		DataSource datasource;

		Connection conn = null;
		Statement stmt = null;
		int count = 0;
		try {
			context = new InitialContext();
			datasource = (DataSource) context
					.lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = datasource.getConnection();
			stmt = conn.createStatement();
			String sql = "SELECT * FROM problem";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				// ExactData uses only ICD9 code.
				String memberID = rs.getString("Member_ID");
				if (memberID.isEmpty()) {
					// If Member_ID is empty, we skip. This should not happen
					// but I noticed empty Member_ID in
					// the problem CSV file.
					continue;
				}

				// Get onset date
				Date dateTime = new java.util.Date(rs.getDate("Onset_Date")
						.getTime());

				condition = createCondition(userInfo.userId + "-" + count + "-"
						+ rs.getInt("ID"), "Patient/" + userInfo.userId,
						"http://hl7.org/fhir/sid/icd-9",
						rs.getString("Problem_Code"),
						rs.getString("Problem_Description"), dateTime);

				retVal.add(condition);
			}

			conn.close();
		} catch (SQLException | NamingException se) {
			// TODO Auto-generated catch block
			se.printStackTrace();
		}
		return retVal;
	}

	public Condition getCondition(String resourceId) {
		String[] Ids = resourceId.split("\\-", 3);
		// Ids[0] -> person id
		// Ids[1] -> count
		// Ids[2] -> condition ID -> eg. 8302-2 for height

		if (Ids.length != 3)
			return null;

		Condition condition = null;
		Connection conn = null;
		DataSource datasource = null;
		Statement stmt = null;
		int count = 0;
		String sql = null;
		ResultSet rs = null;

		try {
			Context context = new InitialContext();
			datasource = (DataSource) context
					.lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = datasource.getConnection();
			stmt = conn.createStatement();

			sql = "SELECT * FROM problem WHERE ID=" + Ids[2];
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				// Get onset date
				Date dateTime = new java.util.Date(rs.getDate("Onset_Date")
						.getTime());

				condition = createCondition(
						Ids[0] + "-" + count + "-" + rs.getInt("ID"),
						"Patient/" + Ids[0], "http://hl7.org/fhir/sid/icd-9",
						rs.getString("Problem_Code"),
						rs.getString("Problem_Description"), dateTime);
			}

			conn.close();
		} catch (SQLException | NamingException se) {
			se.printStackTrace();
		}

		return condition;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.gatech.i3l.HealthPort.HealthPortFHIRIntf#getConditionsByCodeSystem
	 * (java.lang.String, java.lang.String)
	 */
	@Override
	public ArrayList<Condition> getConditionsByCodeSystem(String codeSystem,
			String code) {
		ArrayList<Condition> retVal = new ArrayList<Condition>();

		// Currently, we support only ICD-9. If code system is not specified, we
		// assume it's ICD-9
		if (codeSystem != null
				&& !codeSystem.isEmpty()
				&& !codeSystem
						.equalsIgnoreCase("http://hl7.org/fhir/sid/icd-9")
				&& !codeSystem
						.equalsIgnoreCase("urn:oid:2.16.840.1.113883.6.42")) {
			// return if it's not ICD-9
			return retVal;
		}

		Condition condition = null;
		Connection conn = null;
		Connection conn2 = null;
		Statement stmt = null;
		Statement stmt2 = null;
		Context context = null;
		DataSource datasource = null;
		Context context2 = null;
		DataSource datasource2 = null;

		String personId = null;
		String id = null;
		int count = 0;
		try {

			context = new InitialContext();
			datasource = (DataSource) context
					.lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = datasource.getConnection();
			stmt = conn.createStatement();
			context2 = new InitialContext();
			datasource2 = (DataSource) context2
					.lookup("java:/comp/env/jdbc/HealthPort");
			conn2 = datasource2.getConnection();
			stmt2 = conn2.createStatement();

			String sql = "SELECT * FROM problem WHERE Problem_Code= " + code;
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				personId = rs.getString("Member_ID").trim();
				if (personId.isEmpty())
					continue; // no Member_ID.. just skip this. But, this should
								// be triggered.

				String codeDisplay = rs.getString("Problem_Description");

				Date dateTime = new java.util.Date(rs.getDate("Onset_Date")
						.getTime());
				sql = "SELECT ID FROM USER WHERE PERSONID='" + personId + "'";
				ResultSet temprs = stmt2.executeQuery(sql);
				while (temprs.next()) {
					id = temprs.getString("ID");
					condition = createCondition(
							id + "-" + count + "-" + rs.getInt("ID"),
							"Patient/" + id, "http://hl7.org/fhir/sid/icd-9",
							code, codeDisplay, dateTime);

					retVal.add(condition);
					break; // Only one user. just break out.
				}
			}

			conn.close();
			conn2.close();
		} catch (SQLException | NamingException se) {
			// TODO Auto-generated catch block
			se.printStackTrace();
		}

		return retVal;

	}

	private MedicationPrescription createMedPrescript(String ID,
			String patientRef, String prescriberName, String nameUri,
			String nameCode, int medId, String nameDisp, String doseQty,
			String doseUnit, String medSig, double medQty, int medRefill,
			String status, Date dateTime) {

		MedicationPrescription medicationPrescript = new MedicationPrescription();
		medicationPrescript.setId(ID);

		// Set Patient whom this prescription is for
		ResourceReferenceDt patientRefDt = new ResourceReferenceDt(patientRef);
		medicationPrescript.setPatient(patientRefDt);

		// Prescriber. We don't have Prescriber resource yet. Just display it.
		ResourceReferenceDt providerRefDt = new ResourceReferenceDt();
		providerRefDt.setDisplay(prescriberName);
		medicationPrescript.setPrescriber(providerRefDt);

		// Medication information. We do not have the medication resource yet.
		// So, we put it in the
		// contained resource. We use dispaly as well :)
		// Create Medication Resource
		CodingDt medCoding = new CodingDt(nameUri, nameCode);
		ArrayList<CodingDt> codingList = new ArrayList<CodingDt>();
		codingList.add(medCoding);
		CodeableConceptDt codeDt = new CodeableConceptDt();
		codeDt.setCoding(codingList);

		Medication medResource = new Medication();
		medResource.setCode(codeDt);
		medResource.setId(String.valueOf(medId));
		ArrayList<IResource> medResList = new ArrayList<IResource>();
		medResList.add(medResource);
		ContainedDt medContainedDt = new ContainedDt();
		medContainedDt.setContainedResources(medResList);
		medicationPrescript.setContained(medContainedDt);

		// Medication reference. This should point to the contained resource.
		ResourceReferenceDt medRefDt = new ResourceReferenceDt("#" + medId);
		medRefDt.setDisplay(nameDisp);
		medicationPrescript.setMedication(medRefDt);

		// Dosage Instruction
		DosageInstruction medDosageInstruct = new DosageInstruction();
		doseQty = doseQty.trim();
		try {
			double dQty = Double.parseDouble(doseQty);
			QuantityDt medQtyDt = new QuantityDt(dQty);
			medQtyDt.setUnits(doseUnit);
			medDosageInstruct.setDoseQuantity(medQtyDt);
		} catch (NumberFormatException e) {
			medDosageInstruct.setText("Dose Quanty: " + doseQty);
		}
		medDosageInstruct.setText(medSig);
		ArrayList<DosageInstruction> medDoseInstList = new ArrayList<DosageInstruction>();
		medDoseInstList.add(medDosageInstruct);
		medicationPrescript.setDosageInstruction(medDoseInstList);

		// Dispense Qty and Refill
		Dispense medDispense = new Dispense();
		medDispense.setQuantity(medQty);
		medDispense.setNumberOfRepeatsAllowed(medRefill);
		medicationPrescript.setDispense(medDispense);

		// Date Written
		medicationPrescript.setDateWritten(new DateTimeDt(dateTime));

		// Status
		if (status.equalsIgnoreCase("active")) {
			medicationPrescript
					.setStatus(MedicationPrescriptionStatusEnum.ACTIVE);
		} else if (status.equalsIgnoreCase("completed")) {
			medicationPrescript
					.setStatus(MedicationPrescriptionStatusEnum.COMPLETED);
		} else if (status.equalsIgnoreCase("on_hold")) {
			medicationPrescript
					.setStatus(MedicationPrescriptionStatusEnum.ON_HOLD);
		} else if (status.equalsIgnoreCase("stopped")) {
			medicationPrescript
					.setStatus(MedicationPrescriptionStatusEnum.STOPPED);
		}

		return medicationPrescript;
	}

	public ArrayList<MedicationPrescription> getMedicationPrescriptions(
			HealthPortUserInfo userInfo) {
		ArrayList<MedicationPrescription> retVal = new ArrayList<MedicationPrescription>();
		MedicationPrescription medPrescript = null;
		Context context = null;
		DataSource datasource = null;

		Connection conn = null;
		Statement stmt = null;
		int count = 0;
		try {
			context = new InitialContext();
			datasource = (DataSource) context
					.lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = datasource.getConnection();
			stmt = conn.createStatement();

			String sql = "SELECT * FROM medication_orders WHERE Member_ID = '"
					+ userInfo.personId + "'";
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				// Get onset date
				Date dateTime = new java.util.Date(rs.getDate("Order_Date")
						.getTime());

				medPrescript = createMedPrescript(userInfo.userId + "-" + count
						+ "-" + rs.getString("ID"), "Patient/"
						+ userInfo.userId, rs.getString("Order_Provider_Name"),
						"urn:oid:2.16.840.1.113883.6.69",
						rs.getString("Drug_NDC"), rs.getInt("ID"),
						rs.getString("Drug_Name"), rs.getString("Dose"),
						rs.getString("Units"), rs.getString("Sig"),
						(double) rs.getInt("Qty_Ordered"),
						rs.getInt("Refills"), rs.getString("Status"), dateTime);

				retVal.add(medPrescript);
			}

			conn.close();
		} catch (SQLException | NamingException se) {
			// TODO Auto-generated catch block
			se.printStackTrace();
		}
		return retVal;
	}

	public MedicationPrescription getMedicationPrescription(String resourceId) {
		Context context = null;
		DataSource datasource = null;
		MedicationPrescription medPrescript = null;

		Connection conn = null;
		Statement stmt = null;
		String[] drugDosage = null;
		String drugName = null;
		String lastFilled = null;
		String drugId = null;
		String URL = null;
		String sql = null;
		ResultSet rs = null;
		// String type = null;
		String[] Ids = resourceId.split("\\-", 3);

		if (Ids.length != 3)
			return null;

		int count = 0;
		try {
			context = new InitialContext();
			datasource = (DataSource) context
					.lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = datasource.getConnection();
			stmt = conn.createStatement();
			sql = "SELECT * FROM medication_orders WHERE ID = " + Ids[2];
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Date dateTime = new java.util.Date(rs.getDate("Order_Date")
						.getTime());

				medPrescript = createMedPrescript(Ids[0] + "-" + count + "-"
						+ rs.getString("ID"), "Patient/" + Ids[0],
						rs.getString("Order_Provider_Name"),
						"urn:oid:2.16.840.1.113883.6.69",
						rs.getString("Drug_NDC"), rs.getInt("ID"),
						rs.getString("Drug_Name"), rs.getString("Dose"),
						rs.getString("Units"), rs.getString("Sig"),
						(double) rs.getInt("Qty_Ordered"),
						rs.getInt("Refills"), rs.getString("Status"), dateTime);
			}

			conn.close();
		} catch (SQLException | NamingException se) {
			se.printStackTrace();
		}
		return medPrescript;
	}

	public ArrayList<MedicationPrescription> getMedicationPrescriptionsByType(
			String medName) {
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
			datasource = (DataSource) context
					.lookup("java:/comp/env/jdbc/OMOP");
			conn = datasource.getConnection();
			stmt = conn.createStatement();
			context2 = new InitialContext();
			datasource2 = (DataSource) context2
					.lookup("java:/comp/env/jdbc/HealthPort");
			conn2 = datasource2.getConnection();
			stmt2 = conn2.createStatement();

			String sql = "SELECT drug_exposure_id,person_id, drug_id, drug_name, drug_dosage, last_filled_date FROM drug_exposure WHERE drug_name= "
					+ "\'" + medName + "\'";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				drugDosage = rs.getString("drug_dosage").split("\\s", 2);
				drugId = rs.getString("drug_exposure_id");
				drugConceptId = rs.getString("drug_id");
				drugName = rs.getString("drug_name");
				lastFilled = rs.getString("last_filled_date");
				personId = rs.getString("person_id");

				sql = "SELECT id FROM USER WHERE PERSONID= " + personId;
				ResultSet temprs = stmt2.executeQuery(sql);
				while (temprs.next()) {
					id = temprs.getString("id");

					MedicationPrescription med = new MedicationPrescription();
					med.setId(id + "-" + count + "-" + drugId); // This is
																// object
																// resource ID.
					ResourceReferenceDt subj = new ResourceReferenceDt(
							"Patient/" + id);
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
					// yyyymmdd
					DateTimeDt date = new DateTimeDt(lastFilled.substring(0, 8));
					med.setDateWritten(date);
					med.addIdentifier("NDC", drugConceptId);
					NarrativeStatusEnum narrative = null;
					med.getText().setStatus(narrative.GENERATED);
					StringBuffer buffer_narrative = new StringBuffer();
					buffer_narrative.append("<div>\n");
					buffer_narrative.append("<status value=\"generated\"/>\n");
					buffer_narrative.append("<div class=\"hapiHeaderText\">"
							+ med.getMedication().getDisplay() + "</div>\n");
					buffer_narrative
							.append("<table class=\"hapiPropertyTable\">\n");
					buffer_narrative.append("	<tbody>\n");
					buffer_narrative.append("		<tr>\n");
					buffer_narrative.append("			<td>Medication Name</td>\n");
					buffer_narrative.append("			<td>"
							+ med.getMedication().getDisplay() + "</td>\n");
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