/**
 * 
 */
package edu.gatech.i3l.HealthPort.ports;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.sql.Date;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringEscapeUtils;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu.composite.CodingDt;
import ca.uhn.fhir.model.dstu.composite.ContainedDt;
import ca.uhn.fhir.model.dstu.composite.QuantityDt;
import ca.uhn.fhir.model.dstu.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.Immunization;
import ca.uhn.fhir.model.dstu.resource.Medication;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription.Dispense;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription.DosageInstruction;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.valueset.ConditionStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.ImmunizationRouteCodesEnum;
import ca.uhn.fhir.model.dstu.valueset.MedicationPrescriptionStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationReliabilityEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationStatusEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import edu.gatech.i3l.HealthPort.ConditionSerializable;
import edu.gatech.i3l.HealthPort.HealthPortInfo;
import edu.gatech.i3l.HealthPort.ImmunizationSerializable;
import edu.gatech.i3l.HealthPort.MedicationPrescriptionSerializable;
import edu.gatech.i3l.HealthPort.ObservationSerializable;
import edu.gatech.i3l.HealthPort.PortIf;

/**
 * @author MC142
 *
 */
public class ExactDataPort implements PortIf {

	// static database tags that this class can be used.
	public static String SyntheticEHR = "SyntheticEHR";
	public static String SyntheticCancer = "SyntheticCancer";

	String tag;
	String id;

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

	private DataSource dataSource = null;

	public ExactDataPort(String jndiName, String tag) {
		// tag should be one of defined ones
		if (!tag.equals(SyntheticEHR) && !tag.equals(SyntheticCancer)) {
			// Not valid tag.
			this.tag = null;
			return;
		}

		this.tag = tag;
		try {
			this.id = HealthPortInfo.findIdFromTag(tag);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}

		try {
			dataSource = (DataSource) new InitialContext()
					.lookup("java:/comp/env/" + jndiName);
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Observation from ExactData DB
	 */
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

	public ArrayList<Observation> getVitalSigns(ResultSet rs) {
		ArrayList<Observation> retVal = new ArrayList<Observation>();

		try {
			while (rs.next()) {
				Date dateTime = null;
				Timestamp ts = rs.getTimestamp("Encounter_Date");
				if (ts != null) {
					dateTime = new Date(ts.getTime());
				}

				// Height.
				String height = rs.getString("Height");
				if (!height.isEmpty()) {
					String heightUnit = rs.getString("Height_Units");
					Observation obs = createObs(id + "." + rs.getString("ID")
							+ "-" + Height, "http://loinc.org", heightLOINC,
							"Body Height", height, heightUnit, dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/" + id
							+ "." + rs.getString("Member_ID")));
					retVal.add(obs);
				}

				// Weight.
				String weight = rs.getString("Weight");
				if (!weight.isEmpty()) {
					String weightUnit = rs.getString("Weight_Units");
					Observation obs = createObs(id + "." + rs.getString("ID")
							+ "-" + Weight, "http://loinc.org", weightLOINC,
							"Body Weight", weight, weightUnit, dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/" + id
							+ "." + rs.getString("Member_ID")));
					retVal.add(obs);
				}

				// Respiration
				String respiration = rs.getString("Respiration");
				if (!respiration.isEmpty()) {
					Observation obs = createObs(id + "." + rs.getString("ID")
							+ "-" + Respiration, "http://loinc.org",
							respirationLOINC, "Respiration Rate", respiration,
							"", dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/" + id
							+ "." + rs.getString("Member_ID")));
					retVal.add(obs);
				}

				// Pulse
				String pulse = rs.getString("Pulse");
				if (!pulse.isEmpty()) {
					Observation obs = createObs(id + "." + rs.getString("ID")
							+ "-" + Pulse, "http://loinc.org", pulseLOINC,
							"Heart Beat", pulse, "", dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/" + id
							+ "." + rs.getString("Member_ID")));
					retVal.add(obs);
				}

				// Systolic BP
				String systolicBP = rs.getString("SystolicBP");
				if (!systolicBP.isEmpty()) {
					Observation obs = createObs(id + "." + rs.getString("ID")
							+ "-" + SystolicBP, "http://loinc.org",
							systolicBPLOINC, "Systolic BP", systolicBP,
							"mm[Hg]", dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/" + id
							+ "." + rs.getString("Member_ID")));
					retVal.add(obs);
				}

				// Diastolic BP
				String diastolicBP = rs.getString("DiastolicBP");
				if (!diastolicBP.isEmpty()) {
					Observation obs = createObs(id + "." + rs.getString("ID")
							+ "-" + DiastolicBP, "http://loinc.org",
							diastolicBPLOINC, "Diastolic BP", diastolicBP,
							"mm[Hg]", dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/" + id
							+ "." + rs.getString("Member_ID")));
					retVal.add(obs);
				}

				// Temperature
				String temp = rs.getString("Temperature");
				if (!temp.isEmpty()) {
					String tempUnit = rs.getString("Temperature_Units");
					Observation obs = createObs(id + "." + rs.getString("ID")
							+ "-" + Temperature, "http://loinc.org",
							temperatureLOINC, "Body Temperature", temp,
							tempUnit, dateTime, "");

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/" + id
							+ "." + rs.getString("Member_ID")));
					retVal.add(obs);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return retVal;
	}

	public ArrayList<Observation> getObservationByPatient(String memberID)
			throws SQLException {
		ArrayList<Observation> retVal = new ArrayList<Observation>();
		ArrayList<Observation> obsList;
		// Get all Observations
		Connection conn = null;
		Statement stmt = null;

		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();

			// Get Vital Sign for this patient.
			String sql = "SELECT * FROM vital_sign WHERE Member_ID='"
					+ memberID + "'";
			ResultSet rs = stmt.executeQuery(sql);
			obsList = getVitalSigns(rs);
			if (obsList != null && !obsList.isEmpty()) {
				retVal.addAll(obsList);
			}

			// Get Lab Result
			sql = "SELECT * FROM lab_results WHERE Member_ID='" + memberID
					+ "'";
			rs = stmt.executeQuery(sql);
			obsList = getLabs(rs);
			if (obsList != null && !obsList.isEmpty()) {
				retVal.addAll(obsList);
			}
		} catch (SQLException se) {
			se.printStackTrace();
		} finally {
			conn.close();
		}
		return retVal;
	}

	public Observation getObservation(String resourceId) {
		// ArrayList<String> obsList = new ArrayList<String>();
		String[] Ids = resourceId.split("\\-", 2);

		// Ids[0] -> observation ID
		// Ids[1] -> observation type

		if (Ids.length != 2)
			return null;

		// We know that we are getting observations. Use the
		// reference ID to figure out which observations we need to return
		Connection conn = null;
		Statement stmt = null;
		// String personId = null;
		String sql = null;
		ResultSet rs = null;
		Observation obs = null;

		// Context context = null;
		// DataSource datasource;

		try {
			// context = new InitialContext();
			// datasource = (DataSource) context
			// .lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = dataSource.getConnection();
			stmt = conn.createStatement();

			// URL = url + "/" + dbName;
			// conn = DriverManager.getConnection(URL, username, password);
			// sql =
			// "SELECT observation_id, observation_value, observation_date, observation_concept_id FROM observation WHERE observation_id= "
			// + Ids[2];
			if (Ids[1].equalsIgnoreCase(Lab_Results)) {
				// We are asked to return lab result part of observation.
				// Use the member ID to search and return the observations
				sql = "SELECT * FROM lab_results WHERE ID=" + Ids[0];
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					Date dateTime = new Date(rs.getDate("Date_Resulted")
							.getTime());
					obs = createObs(id + "." + rs.getInt("ID") + "-" + Ids[1],
							"http://loinc.org", rs.getString("Result_LOINC"),
							rs.getString("Result_Name"),
							rs.getString("Numeric_Result"),
							rs.getString("Units"), dateTime,
							rs.getString("Result_Description"));

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/" + id
							+ "." + rs.getString("Member_ID")));
				}
			} else {
				sql = "SELECT * FROM vital_sign WHERE ID=" + Ids[0];
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					Date dateTime = new Date(rs.getDate("Encounter_Date")
							.getTime());
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
						obs = createObs(id + "." + rs.getInt("ID") + "-"
								+ Ids[1], "http://loinc.org", ConceptCode,
								DispName, Value, Units, dateTime, "");

						// Observation Reference to Patient
						obs.setSubject(new ResourceReferenceDt("Patient/" + id
								+ "." + rs.getString("Member_ID")));
					}
				}
			}

			conn.close();
			// } catch (SQLException | NamingException se) {
		} catch (SQLException se) {
			se.printStackTrace();
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
		Statement stmt = null;
		// Context context = null;
		// DataSource datasource = null;
		try {
			// context = new InitialContext();
			// datasource = (DataSource) context
			// .lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
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
					String ID = rs.getString("ID");
					Date dateTime = new Date(rs.getDate("Encounter_Date")
							.getTime());
					if (code.equalsIgnoreCase(heightLOINC)) {
						String height = rs.getString("Height");
						if (!height.isEmpty()) {
							String heightUnit = rs.getString("Height_Units");
							Observation obs = createObs(id + "." + ID + "-"
									+ Height, "http://loinc.org", heightLOINC,
									"Body Height", height, heightUnit,
									dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ id + "." + rs.getString("Member_ID")));
							retVal.add(obs);
						}
					} else if (code.equalsIgnoreCase(weightLOINC)) {
						String weight = rs.getString("Weight");
						if (!weight.isEmpty()) {
							String weightUnit = rs.getString("Weight_Units");
							Observation obs = createObs(id + "." + ID + "-"
									+ Weight, "http://loinc.org", weightLOINC,
									"Body Weight", weight, weightUnit,
									dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ id + "." + rs.getString("Member_ID")));
							retVal.add(obs);
						}
					} else if (code.equalsIgnoreCase(respirationLOINC)) {
						String respiration = rs.getString("Respiration");
						if (!respiration.isEmpty()) {
							Observation obs = createObs(id + "." + ID + "-"
									+ Respiration, "http://loinc.org",
									respirationLOINC, "Respiration Rate",
									respiration, "", dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ id + "." + rs.getString("Member_ID")));
							retVal.add(obs);
						}
					} else if (code.equalsIgnoreCase(pulseLOINC)) {
						String pulse = rs.getString("Pulse");
						if (!pulse.isEmpty()) {
							Observation obs = createObs(id + "." + ID + "-"
									+ Pulse, "http://loinc.org", pulseLOINC,
									"Heart Beat", pulse, "", dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ id + "." + rs.getString("Member_ID")));
							retVal.add(obs);
						}
					} else if (code.equalsIgnoreCase(systolicBPLOINC)) {
						String systolicBP = rs.getString("SystolicBP");
						if (!systolicBP.isEmpty()) {
							Observation obs = createObs(id + "." + ID + "-"
									+ SystolicBP, "http://loinc.org",
									systolicBPLOINC, "Systolic BP", systolicBP,
									"mm[Hg]", dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ id + "." + rs.getString("Member_ID")));
							retVal.add(obs);
						}
					} else if (code.equalsIgnoreCase(diastolicBPLOINC)) {
						String diastolicBP = rs.getString("DiastolicBP");
						if (!diastolicBP.isEmpty()) {
							Observation obs = createObs(id + "." + ID + "-"
									+ DiastolicBP, "http://loinc.org",
									diastolicBPLOINC, "Diastolic BP",
									diastolicBP, "mm[Hg]", dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ id + "." + rs.getString("Member_ID")));
							retVal.add(obs);
						}
					} else if (code.equalsIgnoreCase(temperatureLOINC)) {
						String temp = rs.getString("Temperature");
						if (!temp.isEmpty()) {
							String tempUnit = rs.getString("Temperature_Units");
							Observation obs = createObs(
									id + "." + ID + "-" + Temperature + "-"
											+ rs.getString("Encounter_ID"),
									"http://loinc.org", temperatureLOINC,
									"Body Temperature", temp, tempUnit,
									dateTime, "");

							// Observation Reference to Patient
							obs.setSubject(new ResourceReferenceDt("Patient/"
									+ id + "." + rs.getString("Member_ID")));
							retVal.add(obs);
						}
					}
				}

			} else {
				// No Vital Sign.Check Lab Result.
				// Get Lab Result
				String sql = "SELECT * FROM lab_results WHERE Result_LOINC='"
						+ code + "'";
				ResultSet rs = stmt.executeQuery(sql);
				while (rs.next()) {
					Date dateTime = new Date(rs.getDate("Date_Resulted")
							.getTime());
					Observation obs = createObs(id + "." + rs.getInt("ID")
							+ "-" + Lab_Results, "http://loinc.org",
							rs.getString("Result_LOINC"),
							rs.getString("Result_Name"),
							rs.getString("Numeric_Result"),
							rs.getString("Units"), dateTime,
							rs.getString("Result_Description"));

					// Observation Reference to Patient
					obs.setSubject(new ResourceReferenceDt("Patient/" + id
							+ "." + rs.getString("Member_ID")));
					retVal.add(obs);
				}
			}

			conn.close();
		} catch (SQLException se) {
			se.printStackTrace();
		}
		return retVal;
	}

	public ArrayList<Observation> getLabs(ResultSet rs) {
		ArrayList<Observation> retVal = new ArrayList<Observation>();

		try {
			while (rs.next()) {
				Date dateTime = new Date(rs.getDate("Date_Resulted").getTime());
				Observation obs = createObs(id + "." + rs.getString("ID") + "-"
						+ Lab_Results, "http://loinc.org",
						rs.getString("Result_LOINC"),
						rs.getString("Result_Name"),
						rs.getString("Numeric_Result"), rs.getString("Units"),
						dateTime, rs.getString("Result_Description"));

				// Observation Reference to Patient
				obs.setSubject(new ResourceReferenceDt("Patient/" + id + "."
						+ rs.getString("Member_ID")));
				retVal.add(obs);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return retVal;
	}

	/*
	 * Observation from HealthPort DB
	 */
	private ObservationSerializable createObsSz(String theId, String nameUri,
			String nameCode, String nameDisp, String theValue, String theUnit,
			java.util.Date date, String narrative, String patientId) {
		ObservationSerializable obs = new ObservationSerializable();

		obs.ID = theId;
		obs.NAMEURI = nameUri;
		obs.NAMECODING = nameCode;
		obs.NAMEDISPLAY = nameDisp;
		obs.QUANTITY = theValue;
		obs.UNIT = theUnit;
		obs.SUBJECT = patientId;
		obs.STATUS = "FINAL";
		obs.RELIABILITY = "OK";
		obs.APPLIES = date;
		obs.TEXTSTATUS = "GENERATED";
		obs.NARRATIVE = narrative;

		return obs;
	}

	public List<String> getObservations() throws SQLException {
		List<String> retVal = new ArrayList<String>();
		List<String> obsIds;
		// Get all Observations
		Connection conn = null;
		Statement stmt = null;

		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();

			// Get Vital Sign for this patient.
			String sql = "SELECT * FROM vital_sign";
			ResultSet rs = stmt.executeQuery(sql);
			obsIds = getVitalSignIds(rs);
			if (obsIds != null && !obsIds.isEmpty()) {
				retVal.addAll(obsIds);
			}

			// Get Lab Result
			sql = "SELECT * FROM lab_results";
			rs = stmt.executeQuery(sql);
			obsIds = getLabIds(rs);
			if (obsIds != null && !obsIds.isEmpty()) {
				retVal.addAll(obsIds);
			}
		} catch (SQLException se) {
			se.printStackTrace();
		} finally {
			conn.close();
		}

		return retVal;
	}

	public List<String> getVitalSignIds(ResultSet rs) {
		List<String> retVal = new ArrayList<String>();

		try {
			while (rs.next()) {
				// long longDate = rs.getDate("Encounter_Date").getTime();
				// Date dateTime = new Date(longDate);
				java.util.Date dateTime = null;
				Timestamp ts = rs.getTimestamp("Encounter_Date");
				if (ts != null) {
					dateTime = new java.util.Date(ts.getTime());
				}

				String patientId = "Patient/" + id + "."
						+ rs.getString("Member_ID");

				String height = rs.getString("Height");
				if (!height.isEmpty()) {
					String obsId = id + "." + rs.getString("ID") + "-" + Height;
					String heightUnit = rs.getString("Height_Units");
					String nameDisp = "Body Height";
					String narrative = dateTime.toString() + " " + nameDisp
							+ "=" + height + " " + heightUnit;
					narrative = StringEscapeUtils.escapeHtml4(narrative);

					ObservationSerializable obs = createObsSz(obsId,
							"http://loinc.org", heightLOINC, nameDisp, height,
							heightUnit, dateTime, narrative, patientId);

					HealthPortInfo.storeResource(HealthPortInfo.OBSERVATION,
							obs);
					retVal.add(obsId);
				}

				// Weight.
				String weight = rs.getString("Weight");
				if (!weight.isEmpty()) {
					String weightUnit = rs.getString("Weight_Units");
					String obsId = id + "." + rs.getString("ID") + "-" + Weight;
					String nameDisp = "Body Weight";
					String narrative = dateTime.toString() + " " + nameDisp
							+ "=" + weight + " " + weightUnit;
					narrative = StringEscapeUtils.escapeHtml4(narrative);

					ObservationSerializable obs = createObsSz(obsId,
							"http://loinc.org", weightLOINC, nameDisp, weight,
							weightUnit, dateTime, narrative, patientId);

					HealthPortInfo.storeResource(HealthPortInfo.OBSERVATION,
							obs);
					retVal.add(obsId);
				}

				// Respiration
				String respiration = rs.getString("Respiration");
				if (!respiration.isEmpty()) {
					String obsId = id + "." + rs.getString("ID") + "-"
							+ Respiration;
					String nameDisp = "Respiration Rate";
					String narrative = dateTime.toString() + " " + nameDisp
							+ "=" + respiration;
					narrative = StringEscapeUtils.escapeHtml4(narrative);

					ObservationSerializable obs = createObsSz(obsId,
							"http://loinc.org", respirationLOINC, nameDisp,
							respiration, "", dateTime, narrative, patientId);

					HealthPortInfo.storeResource(HealthPortInfo.OBSERVATION,
							obs);
					retVal.add(obsId);
				}

				// Pulse
				String pulse = rs.getString("Pulse");
				if (!pulse.isEmpty()) {
					String obsId = id + "." + rs.getString("ID") + "-" + Pulse;
					String nameDisp = "Heart Beat";
					String narrative = dateTime.toString() + " " + nameDisp
							+ "=" + pulse;
					narrative = StringEscapeUtils.escapeHtml4(narrative);

					ObservationSerializable obs = createObsSz(obsId,
							"http://loinc.org", pulseLOINC, nameDisp, pulse,
							"", dateTime, narrative, patientId);

					HealthPortInfo.storeResource(HealthPortInfo.OBSERVATION,
							obs);
					retVal.add(obsId);
				}

				// Systolic BP
				String systolicBP = rs.getString("SystolicBP");
				if (!systolicBP.isEmpty()) {
					String obsId = id + "." + rs.getString("ID") + "-"
							+ SystolicBP;
					String nameDisp = "Systolic BP";
					String narrative = dateTime.toString() + " " + nameDisp
							+ "=" + systolicBP;
					narrative = StringEscapeUtils.escapeHtml4(narrative);

					ObservationSerializable obs = createObsSz(obsId,
							"http://loinc.org", systolicBPLOINC, nameDisp,
							systolicBP, "mm[Hg]", dateTime, narrative,
							patientId);

					HealthPortInfo.storeResource(HealthPortInfo.OBSERVATION,
							obs);
					retVal.add(obsId);
				}

				// Diastolic BP
				String diastolicBP = rs.getString("DiastolicBP");
				if (!diastolicBP.isEmpty()) {
					String obsId = id + "." + rs.getString("ID") + "-"
							+ DiastolicBP;
					String nameDisp = "Diastolic BP";
					String narrative = dateTime.toString() + " " + nameDisp
							+ "=" + diastolicBP;
					narrative = StringEscapeUtils.escapeHtml4(narrative);

					ObservationSerializable obs = createObsSz(obsId,
							"http://loinc.org", diastolicBPLOINC, nameDisp,
							diastolicBP, "mm[Hg]", dateTime, narrative,
							patientId);

					HealthPortInfo.storeResource(HealthPortInfo.OBSERVATION,
							obs);
					retVal.add(obsId);
				}

				// Temperature
				String temp = rs.getString("Temperature");
				if (!temp.isEmpty()) {
					String tempUnit = rs.getString("Temperature_Units");
					String obsId = id + "." + rs.getString("ID") + "-"
							+ Temperature;
					String nameDisp = "Body Temperature";
					String narrative = dateTime.toString() + " " + nameDisp
							+ "=" + temp;
					narrative = StringEscapeUtils.escapeHtml4(narrative);

					ObservationSerializable obs = createObsSz(obsId,
							"http://loinc.org", temperatureLOINC, nameDisp,
							temp, tempUnit, dateTime, narrative, patientId);

					HealthPortInfo.storeResource(HealthPortInfo.OBSERVATION,
							obs);
					retVal.add(obsId);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return retVal;
	}

	public List<String> getLabIds(ResultSet rs) {
		List<String> retVal = new ArrayList<String>();

		try {
			while (rs.next()) {
				String obsId = id + "." + rs.getString("ID") + "-"
						+ Lab_Results;
				long longDate = rs.getDate("Date_Resulted").getTime();
				Date dateTime = new Date(longDate);
				String value = rs.getString("Numeric_Result");
				String valueUnit = rs.getString("Units");
				String nameDisp = rs.getString("Result_Name");
				String narrative = dateTime.toString() + " " + nameDisp + "="
						+ value + " " + rs.getString("Result_Description");
				narrative = StringEscapeUtils.escapeHtml4(narrative);

				ObservationSerializable obs = createObsSz(obsId,
						"http://loinc.org", rs.getString("Result_LOINC"),
						nameDisp, value, valueUnit, dateTime, narrative,
						"Patient/" + id + "." + rs.getString("Member_ID"));

				HealthPortInfo.storeResource(HealthPortInfo.OBSERVATION, obs);
				retVal.add(obsId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return retVal;
	}

	/******************************************************************************/

	/*
	 * Conditions from ExactData DB
	 */
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
		// condition.setOnset(new DateTimeDt(dateTime));
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

	public ArrayList<Condition> getConditions(ResultSet rs) {
		ArrayList<Condition> retVal = new ArrayList<Condition>();
		Condition condition = null;

		try {
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
				Date dateTime = new Date(rs.getDate("Onset_Date").getTime());

				condition = createCondition(id + "." + rs.getInt("ID"),
						"Patient/" + id + "." + rs.getString("Member_ID"),
						"http://hl7.org/fhir/sid/icd-9",
						rs.getString("Problem_Code"),
						rs.getString("Problem_Description"), dateTime);

				retVal.add(condition);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return retVal;
	}

	public ArrayList<Condition> getConditionsByPatient(String memberID) {
		ArrayList<Condition> retVal = new ArrayList<Condition>();

		// Context context;
		// DataSource datasource;

		Connection conn = null;
		Statement stmt = null;
		try {
			// context = new InitialContext();
			// datasource = (DataSource) context
			// .lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			String sql = "SELECT * FROM problem WHERE Member_ID='" + memberID
					+ "'";
			ResultSet rs = stmt.executeQuery(sql);
			ArrayList<Condition> conditionList = getConditions(rs);
			if (conditionList != null && !conditionList.isEmpty()) {
				retVal.addAll(conditionList);
			}
			conn.close();
		} catch (SQLException se) {
			se.printStackTrace();
		}
		return retVal;
	}

	public Condition getCondition(String conditionID) {
		Condition condition = null;
		Connection conn = null;
		Statement stmt = null;
		String sql = null;
		ResultSet rs = null;

		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();

			sql = "SELECT * FROM problem WHERE ID=" + conditionID;
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				// Get onset date
				Date dateTime = new Date(rs.getDate("Onset_Date").getTime());

				condition = createCondition(id + "." + rs.getInt("ID"),
						"Patient/" + tag + "." + rs.getString("Member_ID"),
						"http://hl7.org/fhir/sid/icd-9",
						rs.getString("Problem_Code"),
						rs.getString("Problem_Description"), dateTime);
			}

			conn.close();
			// } catch (SQLException | NamingException se) {
		} catch (SQLException se) {
			se.printStackTrace();
		}

		return condition;
	}

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

		Connection conn = null;
		Statement stmt = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			String sql = "SELECT * FROM problem WHERE Problem_Code='" + code
					+ "'";
			ResultSet rs = stmt.executeQuery(sql);
			ArrayList<Condition> conditionList = getConditions(rs);
			if (conditionList != null && !conditionList.isEmpty()) {
				retVal.addAll(conditionList);
			}
			conn.close();
		} catch (SQLException se) {
			se.printStackTrace();
		}

		return retVal;

	}

	/*
	 * Condition from HealthPort DB
	 */
	private ConditionSerializable createConditionSz(String ID, String subjRef,
			String nameUri, String nameCode, String nameDisp,
			java.util.Date dateTime) {

		ConditionSerializable condition = new ConditionSerializable();

		condition.ID = ID;
		condition.SUBJECT = subjRef;
		condition.NAMEURI = nameUri;
		condition.NAMECODING = nameCode;
		condition.NAMEDISPLAY = StringEscapeUtils.escapeHtml4(nameDisp);
		condition.STATUS = "CONFIRMED";
		condition.ONSET = dateTime;

		String textBody = "<table>"
				+ "<tr><td>Problem</td><td>Onset Date</td><td>ICD-9</td></tr>"
				+ "<tr><td>" + nameDisp + "</td>" + "<td>"
				+ dateTime.toString() + "</td>" + "<td>" + nameCode
				+ "</td></tr></table>";
		condition.NARRATIVE = textBody;
		condition.TEXTSTATUS = "GENERATED";

		return condition;
	}

	public List<String> getConditions() {
		List<String> retVal = new ArrayList<String>();

		Connection conn = null;
		Statement stmt = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			String sql = "SELECT * FROM problem";
			ResultSet rs = stmt.executeQuery(sql);
			List<String> conditionList = getConditionIds(rs);
			if (conditionList != null && !conditionList.isEmpty()) {
				retVal.addAll(conditionList);
			}
			conn.close();
		} catch (SQLException se) {
			se.printStackTrace();
		}
		return retVal;
	}

	public List<String> getConditionIds(ResultSet rs) {
		List<String> retVal = new ArrayList<String>();
		ConditionSerializable condition = null;

		try {
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
				java.util.Date dateTime = null;
				Timestamp ts = rs.getTimestamp("Onset_Date");
				if (ts != null) {
					dateTime = new java.util.Date(ts.getTime());
				}

				String condId = id + "." + rs.getInt("ID");
				condition = createConditionSz(condId, "Patient/" + id + "."
						+ rs.getString("Member_ID"),
						"http://hl7.org/fhir/sid/icd-9",
						rs.getString("Problem_Code"),
						rs.getString("Problem_Description"), dateTime);

				HealthPortInfo.storeResource(HealthPortInfo.CONDITION,
						condition);

				retVal.add(condId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return retVal;
	}

	/*******************************************************************/

	// MedicationPrescription Resource from ExactData DB
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
		List<CodingDt> codingList = new ArrayList<CodingDt>();
		codingList.add(medCoding);
		CodeableConceptDt codeDt = new CodeableConceptDt();
		codeDt.setCoding(codingList);

		Medication medResource = new Medication();
		medResource.setCode(codeDt);
		medResource.setId(String.valueOf(medId));
		List<IResource> medResList = new ArrayList<IResource>();
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
			// medDosageInstruct.setDose(medQtyDt);
		} catch (NumberFormatException e) {
			medDosageInstruct.setText("Dose Quanty: " + doseQty);
		}
		medDosageInstruct.setText(medSig);
		ArrayList<DosageInstruction> medDoseInstList = new ArrayList<DosageInstruction>();
		medDoseInstList.add(medDosageInstruct);
		medicationPrescript.setDosageInstruction(medDoseInstList);

		// Dispense Qty and Refill
		Dispense medDispense = new Dispense();
		medDispense.setQuantity(new QuantityDt(medQty));
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
			ResultSet rs) {
		ArrayList<MedicationPrescription> retVal = new ArrayList<MedicationPrescription>();
		MedicationPrescription medPrescript = null;

		try {
			while (rs.next()) {
				String memberID = rs.getString("Member_ID").trim();
				// Get onset date
				Date dateTime = new Date(rs.getDate("Order_Date").getTime());

				medPrescript = createMedPrescript(
						id + "." + rs.getString("ID"), "Patient/" + id + "."
								+ memberID,
						rs.getString("Order_Provider_Name"),
						"urn:oid:2.16.840.1.113883.6.69",
						rs.getString("Drug_NDC"), rs.getInt("ID"),
						rs.getString("Drug_Name"), rs.getString("Dose"),
						rs.getString("Units"), rs.getString("Sig"),
						(double) rs.getInt("Qty_Ordered"),
						rs.getInt("Refills"), rs.getString("Status"), dateTime);

				retVal.add(medPrescript);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return retVal;
	}

	public ArrayList<MedicationPrescription> getMedicationPrescriptionByPatient(
			String memberID) {
		ArrayList<MedicationPrescription> retVal = new ArrayList<MedicationPrescription>();
		// Context context = null;
		// DataSource datasource = null;

		Connection conn = null;
		Statement stmt = null;
		try {
			// context = new InitialContext();
			// datasource = (DataSource) context
			// .lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = dataSource.getConnection();
			stmt = conn.createStatement();

			String sql = "SELECT * FROM medication_orders where Member_ID='"
					+ memberID + "'";
			ResultSet rs = stmt.executeQuery(sql);
			ArrayList<MedicationPrescription> medList = getMedicationPrescriptions(rs);
			if (medList != null && !medList.isEmpty()) {
				retVal.addAll(medList);
			}

			conn.close();
		} catch (SQLException se) {
			se.printStackTrace();
		}
		return retVal;
	}

	public MedicationPrescription getMedicationPrescription(String medID) {
		// Context context = null;
		// DataSource datasource = null;
		MedicationPrescription medPrescript = null;

		Connection conn = null;
		Statement stmt = null;
		String sql = null;
		ResultSet rs = null;
		// String type = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			sql = "SELECT * FROM medication_orders WHERE ID = " + medID;
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String memberID = rs.getString("Member_ID").trim();
				Date dateTime = new Date(rs.getDate("Order_Date").getTime());

				medPrescript = createMedPrescript(
						id + "." + rs.getString("ID"), "Patient/" + tag + "."
								+ memberID,
						rs.getString("Order_Provider_Name"),
						"urn:oid:2.16.840.1.113883.6.69",
						rs.getString("Drug_NDC"), rs.getInt("ID"),
						rs.getString("Drug_Name"), rs.getString("Dose"),
						rs.getString("Units"), rs.getString("Sig"),
						(double) rs.getInt("Qty_Ordered"),
						rs.getInt("Refills"), rs.getString("Status"), dateTime);
			}

			conn.close();
			// } catch (SQLException | NamingException se) {
		} catch (SQLException se) {
			se.printStackTrace();
		}
		return medPrescript;
	}

	// This is searching by medication name.
	public ArrayList<MedicationPrescription> getMedicationPrescriptionsByType(
			String medName) {
		ArrayList<MedicationPrescription> retVal = new ArrayList<MedicationPrescription>();

		Connection conn = null;
		Statement stmt = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();

			String sql = "SELECT * FROM medication_orders WHERE Drug_Name LIKE "
					+ "'%" + medName + "%'";
			ResultSet rs = stmt.executeQuery(sql);
			ArrayList<MedicationPrescription> medList = getMedicationPrescriptions(rs);
			if (medList != null && !medList.isEmpty()) {
				retVal.addAll(medList);
			}

			conn.close();
		} catch (SQLException se) {
			se.printStackTrace();
		}
		return retVal;
	}

	// MedicationPrescription Resource from ExactData DB
	private MedicationPrescriptionSerializable createMedPrescriptSz(String ID,
			String patientRef, String prescriberName, String nameUri,
			String nameCode, String nameDisp, String doseQty, String doseUnit,
			String medSig, int medQty, int medRefill, String status,
			java.util.Date dateTime) {

		MedicationPrescriptionSerializable medicationPrescript = new MedicationPrescriptionSerializable();

		medicationPrescript.ID = ID;
		medicationPrescript.SUBJECT = patientRef;
		medicationPrescript.PRESCRIBER = prescriberName;
		medicationPrescript.NAMEURI = nameUri;
		medicationPrescript.NAMECODING = nameCode;
		medicationPrescript.NAMEDISPLAY = nameDisp;
		medicationPrescript.DOSEQTY = doseQty.trim();
		medicationPrescript.DOSEUNIT = doseUnit;
		medicationPrescript.DOSAGEINSTRUCTION = medSig;
		medicationPrescript.DISPENSEQTY = medQty;
		medicationPrescript.REFILL = medRefill;
		medicationPrescript.DATEWRITTEN = dateTime;
		medicationPrescript.STATUS = status;
		return medicationPrescript;
	}

	public List<String> getMedicationPrescriptionIds(ResultSet rs) {
		List<String> retVal = new ArrayList<String>();
		MedicationPrescriptionSerializable medPrescript = null;

		try {
			while (rs.next()) {
				String memberID = rs.getString("Member_ID").trim();
				// Get onset date
				// Date dateTime = new Date(rs.getDate("Order_Date").getTime());
				java.util.Date dateTime = null;
				Timestamp ts = rs.getTimestamp("Order_Date");
				if (ts != null) {
					dateTime = new java.util.Date(ts.getTime());
				}

				String medPresId = id + "." + rs.getString("ID");
				medPrescript = createMedPrescriptSz(medPresId, "Patient/" + id
						+ "." + memberID, rs.getString("Order_Provider_Name"),
						"urn:oid:2.16.840.1.113883.6.69",
						rs.getString("Drug_NDC"), rs.getString("Drug_Name"),
						rs.getString("Dose"), rs.getString("Units"),
						rs.getString("Sig"), rs.getInt("Qty_Ordered"),
						rs.getInt("Refills"), rs.getString("Status"), dateTime);

				HealthPortInfo.storeResource(
						HealthPortInfo.MEDICATIONPRESCRIPTION, medPrescript);

				retVal.add(medPresId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return retVal;
	}

	public List<String> getMedicationPrescriptions() {
		List<String> retVal = new ArrayList<String>();
		// Context context = null;
		// DataSource datasource = null;

		Connection conn = null;
		Statement stmt = null;
		try {
			// context = new InitialContext();
			// datasource = (DataSource) context
			// .lookup("java:/comp/env/jdbc/ExactDataSample");
			conn = dataSource.getConnection();
			stmt = conn.createStatement();

			String sql = "SELECT * FROM medication_orders";
			ResultSet rs = stmt.executeQuery(sql);
			List<String> medList = getMedicationPrescriptionIds(rs);
			if (medList != null && !medList.isEmpty()) {
				retVal.addAll(medList);
			}

			conn.close();
			// } catch (SQLException | NamingException se) {
		} catch (SQLException se) {
			se.printStackTrace();
		}
		return retVal;
	}


	/*******************************************************************/

	// create HAPI-FHIR Immunization object
	private Immunization createImmunization(String id, String nameUri, String nameCode, String nameDisp,
			 String subjRef, java.util.Date vacDate, String series, String manufacturer, String lot,
			 String doseQty, String doseUnits, String site, String route, String performerId, String performerName,
			 String encId) {
		Immunization immu = new Immunization();
		
		// set ID
		immu.setId(id);

		// set VaccineType
		nameDisp = StringEscapeUtils.escapeHtml4(nameDisp);
		CodingDt nameCoding = new CodingDt();
		nameCoding.setValueSet(new ResourceReferenceDt("http://hl7.org/fhir/vs/vaccinetype"));
		nameCoding.setSystem(nameUri); // typically "http://www2a.cdc.gov/vaccines/iis/iisstandards/vaccines.asp?rpt=cvx"
		nameCoding.setCode(nameCode);
		nameCoding.setDisplay(nameDisp);
		ArrayList<CodingDt> codingList = new ArrayList<CodingDt>();
		codingList.add(nameCoding);
		CodeableConceptDt codeDt = new CodeableConceptDt();
		codeDt.setCoding(codingList);
		immu.setVaccineType(codeDt);

		// set Subject as reference to Patient
		ResourceReferenceDt subj = new ResourceReferenceDt(subjRef);
		immu.setSubject(subj);

		// set Date
		if (vacDate != null)
			immu.setDate(new DateTimeDt(vacDate));

		// set VaccinationProtocol.series
		Immunization.VaccinationProtocol ivp = new Immunization.VaccinationProtocol();
		ivp.setSeries(series);
		ArrayList<Immunization.VaccinationProtocol> ivpList = new ArrayList<Immunization.VaccinationProtocol>();
		ivpList.add(ivp);
		immu.setVaccinationProtocol(ivpList);
		
		// set manufacturer = resource reference
		if (!manufacturer.equalsIgnoreCase("other"))
			immu.setManufacturer(new ResourceReferenceDt(manufacturer));
		
		// set lot number
		immu.setLotNumber(lot);
		
		// set DoseQuantity, value and units
		QuantityDt qty = new QuantityDt(new Double(doseQty));
		qty.setUnits(doseUnits);
		immu.setDoseQuantity(qty);
		
		// set site - setSite(CodeableConceptDt theValue)
		CodingDt siteCode = new CodingDt();
		siteCode.setValueSet(new ResourceReferenceDt("http://hl7.org/fhir/vs/immunization-site"));
		siteCode.setSystem("http://hl7.org/fhir/v3/ActSite");
		siteCode.setDisplay(site);
		ArrayList<CodingDt> siteCodeList = new ArrayList<CodingDt>();
		siteCodeList.add(siteCode);
		CodeableConceptDt siteConcept = new CodeableConceptDt();
		siteConcept.setCoding(siteCodeList);
		
		// set route - setRoute(ImmunizationRouteCodesEnum theValue)
		if (route.matches("(?i:im|intramuscular)"))
			immu.setRoute(ImmunizationRouteCodesEnum.IM);
		else if (route.matches("(?i:nasinhl|nasal|inhalation)"))
			immu.setRoute(ImmunizationRouteCodesEnum.NASINHL);
		else if (route.matches("(?i:po|oral|by mouth)"))
			immu.setRoute(ImmunizationRouteCodesEnum.PO);

		// set provider, id and display name = resource reference
		ResourceReferenceDt performer = new ResourceReferenceDt(performerId);
		performer.setDisplay(performerName);
		immu.setPerformer(performer);
		
		/*
		 *  DSTU1 has no option to set encounter_id (resource reference) on the Immunization resource.
		 *  
		 *  However, it is likely to be implemented in DSTU2:
		 *  @see http://hl7.org/fhir/2015May/immunization.html
		 *  
		 *  ...and is alreaday in HAPI-FHIR's DSTU-2:
		 *  @see https://jamesagnew.github.io/hapi-fhir/apidocs-dstu2/ca/uhn/fhir/model/dstu2/resource/Immunization.html
		 *  
		 *  DSTU2 example:
		 *  	immu.setEncounter(new ResourceReferenceDt(rs.getString("ENCOUNTER_ID")));
		 */

		return immu;
	}
	
	// create immunization object in internal serializable form
	private ImmunizationSerializable createImmunizationSz(String id, String nameUri, String nameCode, String nameDisp,
			 String subjRef, java.util.Date vacDate, String series, String manufacturer, String lot,
			 String doseQty, String doseUnits, String site, String route, String performerId, String performerName,
			 String encId) {
		ImmunizationSerializable immu = new ImmunizationSerializable();
		
		immu.ID = id;
		immu.NAMEURI = nameUri;
		immu.NAMECODING = nameCode;
		immu.NAMEDISPLAY = nameDisp;
		immu.SUBJECT = subjRef;
		immu.VACCINATION_DATE = vacDate;
		immu.MANUFACTURER = manufacturer;
		immu.LOT_NUMBER = lot;
		immu.DOSE_QUANTITY = doseQty;
		immu.DOSE_UNITS = doseUnits;
		immu.SITE = site;
		immu.ROUTE = route;
		immu.PERFORMER_ID = performerId;
		immu.PERFORMER_NAME = performerName;
		immu.ENCOUNTER_ID = encId;
		
		return immu;
	}
	
	// get all available immunization ids, stored in HealthPort DB as a side-effect
	public List<String> getImmunizations() {
		List<String> retVal = new ArrayList<String>();

		Connection conn = null;
		Statement stmt = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			String sql = "SELECT * FROM immunization";
			ResultSet rs = stmt.executeQuery(sql);
			List<String> immuList = getImmunizationIds(rs);
			if (immuList != null && !immuList.isEmpty()) {
				retVal.addAll(immuList);
			}
			conn.close();
		} catch (SQLException se) {
			se.printStackTrace();
		}
		return retVal;
	}
	
	// get immunizations ids from a ResultSet, storing them in HealthPort DB as a side-effect
	public List<String> getImmunizationIds(ResultSet rs) {
		List<String> retVal = new ArrayList<String>();
		ImmunizationSerializable immu = null;

		try {
			while (rs.next()) {
				String memberID = rs.getString("Member_ID");
				if (memberID.isEmpty()) {
					continue;
				}

				/* prep id -  ExactData's immunization tables do not have ids, so we must create our own
				 * built from encounter_id + CVX code
				 * hopefully it's safe to assume you don't get the same vaccine twice on the same visit?
				 */
				String immuId = rs.getString("Encounter_ID") + "." + rs.getString("Vaccine_CVX");
				
				// prep date
				java.util.Date vacDate = null;
				Timestamp ts = rs.getTimestamp("Vaccination_Date");
				if (ts != null) {
					vacDate = new java.util.Date(ts.getTime());
				}

				immu = createImmunizationSz(
						immuId,
						"http://www2a.cdc.gov/vaccines/iis/iisstandards/vaccines.asp?rpt=cvx",
						rs.getString("Vaccine_CVX"),
						rs.getString("Vaccine_Name"),
						"Patient/" + id + "." + rs.getString("Member_ID"),
						vacDate,
						rs.getString("Series"),
						rs.getString("Manufacturer"),
						rs.getString("Lot_Number"),
						rs.getString("Dose"),
						rs.getString("Units"),
						rs.getString("Site"),
						rs.getString("Route"),
						rs.getString("Provider_ID"),
						rs.getString("Provider_Name"),
						rs.getString("Encounter_ID")
						);

				HealthPortInfo.storeResource(HealthPortInfo.IMMUNIZATION, immu);

				retVal.add(immuId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return retVal;
	}

	// get specific HAPI-FHIR Immunization objects from a ResultSet, used by getImmunizationsByPatient()
	public ArrayList<Immunization> getImmunizations(ResultSet rs) {
		ArrayList<Immunization> retVal = new ArrayList<Immunization>();

		try {
			while (rs.next()) {
				String memberID = rs.getString("Member_ID");
				if (memberID.isEmpty()) {
					continue;
				}

				// prep date
				java.util.Date vacDate = null;
				Timestamp ts = rs.getTimestamp("Vaccination_Date");
				if (ts != null) {
					vacDate = new java.util.Date(ts.getTime());
				}

				String immuId = id + "." + rs.getInt("ID");
				Immunization immu = createImmunization(
						immuId,
						rs.getString("Vaccine_CVX"),
						"http://www2a.cdc.gov/vaccines/iis/iisstandards/vaccines.asp?rpt=cvx",
						rs.getString("Vaccine_Name"),
						"Patient/" + id + "." + rs.getString("Member_ID"),
						vacDate,
						rs.getString("Series"),
						rs.getString("Manufacturer"),
						rs.getString("Lot_Number"),
						rs.getString("Dose"),
						rs.getString("Units"),
						rs.getString("Site"),
						rs.getString("Route"),
						rs.getString("Provider_ID"),
						rs.getString("Provider_Name"),
						rs.getString("Encounter_ID")
						);
				retVal.add(immu);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return retVal;
	}

	// get HAPI-FHIR Immunization objects for a specific patient, not currently called anywhere
	public ArrayList<Immunization> getImmunizationsByPatient(String memberID) {
		ArrayList<Immunization> retVal = new ArrayList<Immunization>();

		Connection conn = null;
		Statement stmt = null;
		try {
			conn = dataSource.getConnection();
			stmt = conn.createStatement();
			String sql = "SELECT * FROM immunization WHERE Member_ID='" + memberID + "'";
			ResultSet rs = stmt.executeQuery(sql);

			ArrayList<Immunization> immuList = getImmunizations(rs);
			if (immuList != null && !immuList.isEmpty()) {
				retVal.addAll(immuList);
			}

			conn.close();
		} catch (SQLException se) {
			se.printStackTrace();
		}
		return retVal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.gatech.i3l.HealthPort.PortIf#getTag()
	 */
	@Override
	public String getTag() {
		return tag;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.gatech.i3l.HealthPort.PortIf#getId()
	 */
	@Override
	public String getId() {
		return id;
	}

}