/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import ca.uhn.fhir.model.dstu.resource.Medication;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription.Dispense;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription.DosageInstruction;
import ca.uhn.fhir.model.dstu.valueset.ConditionStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.MedicationPrescriptionStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationReliabilityEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationStatusEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;

/**
 * @author Myung Choi Constructor requires UserID. - Connects to SQL database to
 *         obtain patient related parameters.
 */
public class HealthPortInfo {
	public String userId = null;
	public String name = null;
	public String recordId = null;
	public String personId = null;
	public String source = null;
	public String orgID = null;
	public String gender = null;
	public String contact = null;
	public String address = null;

	// Database Paging Resource Names
	public static String OBSERVATION = "OBSERVATION";
	public static String CONDITION = "CONDITIONS";
	public static String MEDICATIONPRESCRIPTION = "MEDICATIONPRESCRIPTION";

	private DataSource dataSource;

	/**
	 * 
	 */
	public HealthPortInfo() {
		databaseSetup("jdbc/HealthPort");
	}

	public HealthPortInfo(String jndiName) {
		databaseSetup(jndiName);
	}

	public HealthPortInfo(String jndiName, String userId) {
		databaseSetup(jndiName);
		setInformation(userId);
	}

	public List<String> getResourceIdsByPatient(String tableName,
			String patientId) {
		List<String> retVal = new ArrayList<String>();

		Connection connection = null;
		Statement statement = null;

		String SQL_STATEMENT = "SELECT ID FROM " + tableName
				+ " WHERE SUBJECT = 'Patient/" + patientId + "'";

		System.out.println ("HealthPortInfo: getResourceIdsByPatient: "+tableName+" for Patient "+patientId);
		connection = getConnection();
		try {
			statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(SQL_STATEMENT);

			while (rs.next()) {
				retVal.add(rs.getString("ID"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println ("HealthPortInfo: getResourceIdsByPatient: Done");

		return retVal;
	}

	public List<String> getResourceIdsByCodeSystem(String tableName,
			String codeSystem, String code) {
		List<String> retVal = new ArrayList<String>();

		Connection connection = null;
		Statement statement = null;

		String SQL_STATEMENT = "SELECT ID FROM " + tableName
				+ " WHERE NAMECODING = '" + code + "'";

		System.out.println ("HealthPortInfo: getResourceIdsByCodeSystem: "+tableName+" for codesys/code "+codeSystem+"/"+code);
		connection = getConnection();

		try {
			statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(SQL_STATEMENT);

			while (rs.next()) {
				retVal.add(rs.getString("ID"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		System.out.println ("HealthPortInfo: getResourceIdsByCodeSystem: done");

		return retVal;
	}

	public List<String> getAllResourceIds(String tableName) {
		List<String> retVal = new ArrayList<String>();

		Connection connection = null;
		Statement statement = null;

		String SQL_STATEMENT = "SELECT ID FROM " + tableName;

		System.out.println ("HealthPortInfo: getAllResoureIds: "+tableName);
		connection = getConnection();
		try {
			statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(SQL_STATEMENT);

			while (rs.next()) {
				retVal.add(rs.getString("ID"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		System.out.println ("HealthPortInfo: getAllResoureIds: done");
		return retVal;
	}

	public static void storeResource(String tableName, Object obj0)
			throws SQLException {
		Connection connection = null;
		String SQL_STATEMENT;

		try {
			DataSource ds = (DataSource) new InitialContext()
					.lookup("java:/comp/env/jdbc/HealthPort");

			connection = ds.getConnection();
			if (tableName.equals(OBSERVATION)) {
				ObservationSerializable obj = (ObservationSerializable) obj0;
				SQL_STATEMENT = "REPLACE INTO "
						+ tableName
						+ " (ID, NAMEURI, NAMECODING, NAMEDISPLAY, QUANTITY, UNIT, COMMENT, SUBJECT, STATUS, RELIABILITY, APPLIES, ISSUED, TEXTSTATUS, NARRATIVE) VALUES "
						+ " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				PreparedStatement pstmt = connection
						.prepareStatement(SQL_STATEMENT);

				// set input parameters
				pstmt.setString(1, obj.ID);
				pstmt.setString(2, obj.NAMEURI);
				pstmt.setString(3, obj.NAMECODING);
				pstmt.setString(4, obj.NAMEDISPLAY);
				pstmt.setString(5, obj.QUANTITY);
				pstmt.setString(6, obj.UNIT);
				pstmt.setString(7, obj.COMMENT);
				pstmt.setString(8, obj.SUBJECT);
				pstmt.setString(9, obj.STATUS);
				pstmt.setString(10, obj.RELIABILITY);
				if (obj.APPLIES != null) {
					Timestamp ts = new Timestamp(obj.APPLIES.getTime());
					pstmt.setTimestamp(11, ts);
				} else {
					pstmt.setTimestamp(11, null);
				}
				if (obj.ISSUED != null) {
					Timestamp ts = new Timestamp(obj.ISSUED.getTime());
					pstmt.setTimestamp(12, ts);
				} else {
					pstmt.setTimestamp(12, null);
				}
				pstmt.setString(13, obj.TEXTSTATUS);
				pstmt.setString(14, obj.NARRATIVE);
				pstmt.executeUpdate();
				pstmt.clearParameters();
				pstmt.close();
			} else if (tableName.equals(CONDITION)) {
				ConditionSerializable obj = (ConditionSerializable) obj0;
				SQL_STATEMENT = "REPLACE INTO "
						+ tableName
						+ " (ID,NAMEURI,NAMECODING,NAMEDISPLAY,SUBJECT,STATUS,ONSET,DATEASSERTED,TEXTSTATUS,NARRATIVE) VALUES "
						+ " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				PreparedStatement pstmt = connection
						.prepareStatement(SQL_STATEMENT);

				// set input parameters
				pstmt.setString(1, obj.ID);
				pstmt.setString(2, obj.NAMEURI);
				pstmt.setString(3, obj.NAMECODING);
				pstmt.setString(4, obj.NAMEDISPLAY);
				pstmt.setString(5, obj.SUBJECT);
				pstmt.setString(6, obj.STATUS);
				if (obj.ONSET != null) {
					Timestamp ts = new Timestamp(obj.ONSET.getTime());
					pstmt.setTimestamp(7, ts);
				} else {
					pstmt.setTimestamp(7, null);
				}
				if (obj.DATEASSERTED != null) {
					Timestamp ts = new Timestamp(obj.DATEASSERTED.getTime());
					pstmt.setTimestamp(8, ts);
				} else {
					pstmt.setTimestamp(8, null);
				}
				pstmt.setString(9, obj.TEXTSTATUS);
				pstmt.setString(10, obj.NARRATIVE);

				pstmt.executeUpdate();
				pstmt.clearParameters();
				pstmt.close();

			} else if (tableName.equals(MEDICATIONPRESCRIPTION)) {
				MedicationPrescriptionSerializable obj = (MedicationPrescriptionSerializable) obj0;
				SQL_STATEMENT = "REPLACE INTO "
						+ tableName
						+ " (ID,NAMEURI,NAMECODING,NAMEDISPLAY,SUBJECT,PRESCRIBER,DOSEQTY,DOSEUNIT,DOSAGEINSTRUCTION,DISPENSEQTY,REFILL,DATEWRITTEN,STATUS,TEXTSTATUS,NARRATIVE) VALUES "
						+ " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,? ,?)";
				PreparedStatement pstmt = connection
						.prepareStatement(SQL_STATEMENT);

				// set input parameters
				pstmt.setString(1, obj.ID);
				pstmt.setString(2, obj.NAMEURI);
				pstmt.setString(3, obj.NAMECODING);
				pstmt.setString(4, obj.NAMEDISPLAY);
				pstmt.setString(5, obj.SUBJECT);
				pstmt.setString(6, obj.PRESCRIBER);
				pstmt.setString(7, obj.DOSEQTY);
				pstmt.setString(8, obj.DOSEUNIT);
				pstmt.setString(9, obj.DOSAGEINSTRUCTION);
				pstmt.setInt(10, obj.DISPENSEQTY);
				pstmt.setInt(11, obj.REFILL);
				if (obj.DATEWRITTEN != null) {
					Timestamp ts = new Timestamp(obj.DATEWRITTEN.getTime());
					pstmt.setTimestamp(12, ts);
				} else {
					pstmt.setTimestamp(12, null);
				}
				pstmt.setString(13, obj.STATUS);
				pstmt.setString(14, obj.TEXTSTATUS);
				pstmt.setString(15, obj.NARRATIVE);

				pstmt.executeUpdate();
				pstmt.clearParameters();
				pstmt.close();

			}
		} catch (NamingException | SQLException e) {
			e.printStackTrace();
		} finally {
			connection.close();
		}
	}

	public List<IResource> getResourceList(String tableName, List<String> Ids)
			throws SQLException {
		List<IResource> retVal = new ArrayList<IResource>();
		Connection connection = null;
		Statement getRes = null;

		try {
//			DataSource ds = (DataSource) new InitialContext()
//					.lookup("java:/comp/env/jdbc/HealthPort");
//
//			connection = ds.getConnection();
			connection = getConnection();
			getRes = connection.createStatement();
			// System.out.println("reading from OBSERVATION: "+Ids.size());
			for (String resId : Ids) {
				String SQL_STATEMENT = "SELECT * FROM " + tableName
						+ " WHERE ID='" + resId + "'";
				ResultSet rs = getRes.executeQuery(SQL_STATEMENT);
				if (rs.next()) {
					// Create observations
					if (tableName.equals(OBSERVATION)) {
						String theId = rs.getString("ID");
						String nameUri = rs.getString("NAMEURI");
						String nameCode = rs.getString("NAMECODING");
						String nameDisp = rs.getString("NAMEDISPLAY");
						String theValue = rs.getString("QUANTITY");
						String theUnit = rs.getString("UNIT");
						String textBlock = rs.getString("NARRATIVE");
						String patientId = rs.getString("SUBJECT");
						String comment = rs.getString("COMMENT");
						Timestamp issuedTS = rs.getTimestamp("ISSUED");
						java.util.Date issuedDate = null;
						if (issuedTS != null) {
							issuedDate = new Date(issuedTS.getTime());
						}
						Timestamp appliedTS = rs.getTimestamp("APPLIES");
						java.util.Date appliedDate = null;
						if (appliedTS != null) {
							appliedDate = new Date(appliedTS.getTime());
						}

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
						if (theValue != null && !theValue.isEmpty()) {
							// Remove any commas
							theValue = theValue.replace(",", "");
							QuantityDt qDt = new QuantityDt(
									Double.parseDouble(theValue));
							if (!theUnit.isEmpty()) {
								qDt.setUnits(theUnit);
								// qDt.setSystem(vUri); These are optional...
								// qDt.setCode(vCode);
							}

							obs.setValue(qDt);
						}
						// Observation Status
						if (rs.getString("STATUS").equalsIgnoreCase("AMENDED")) {
							obs.setStatus(ObservationStatusEnum.AMENDED);
						} else if (rs.getString("STATUS").equalsIgnoreCase(
								"CANCELLED")) {
							obs.setStatus(ObservationStatusEnum.CANCELLED);
						} else if (rs.getString("STATUS").equalsIgnoreCase(
								"ENTERED_IN_ERROR")) {
							obs.setStatus(ObservationStatusEnum.ENTERED_IN_ERROR);
						} else if (rs.getString("STATUS").equalsIgnoreCase(
								"PRELIMINARY")) {
							obs.setStatus(ObservationStatusEnum.PRELIMINARY);
						} else if (rs.getString("STATUS").equalsIgnoreCase(
								"REGISTERED")) {
							obs.setStatus(ObservationStatusEnum.REGISTERED);
						} else {
							obs.setStatus(ObservationStatusEnum.FINAL);
						}

						// Reliability
						if (rs.getString("RELIABILITY").equalsIgnoreCase(
								"CALIBRATING")) {
							obs.setReliability(ObservationReliabilityEnum.CALIBRATING);
						} else if (rs.getString("RELIABILITY")
								.equalsIgnoreCase("EARLY")) {
							obs.setReliability(ObservationReliabilityEnum.EARLY);
						} else if (rs.getString("RELIABILITY")
								.equalsIgnoreCase("ERROR")) {
							obs.setReliability(ObservationReliabilityEnum.ERROR);
						} else if (rs.getString("RELIABILITY")
								.equalsIgnoreCase("ONGOING")) {
							obs.setReliability(ObservationReliabilityEnum.ONGOING);
						} else if (rs.getString("RELIABILITY")
								.equalsIgnoreCase("QUESTIONABLE")) {
							obs.setReliability(ObservationReliabilityEnum.QUESTIONABLE);
						} else if (rs.getString("RELIABILITY")
								.equalsIgnoreCase("UNKNOWN")) {
							obs.setReliability(ObservationReliabilityEnum.UNKNOWN);
						} else {
							obs.setReliability(ObservationReliabilityEnum.OK);
						}

						if (issuedDate != null) {
							// System.out.println("ISSUED DATE:"+issuedDate);
							obs.setIssuedWithMillisPrecision(issuedDate);
						}

						if (appliedDate != null) {
							obs.setApplies(new DateTimeDt(appliedDate));
						}

						obs.setSubject(new ResourceReferenceDt(patientId));
						if (comment != null && !comment.isEmpty()) {
							obs.setComments(comment);
						}

						// Human Readable Section
						if (rs.getString("TEXTSTATUS").equalsIgnoreCase(
								"ADDITIONAL")) {
							obs.getText().setStatus(
									NarrativeStatusEnum.ADDITIONAL);
						} else if (rs.getString("TEXTSTATUS").equalsIgnoreCase(
								"EMPTY")) {
							obs.getText().setStatus(NarrativeStatusEnum.EMPTY);
						} else if (rs.getString("TEXTSTATUS").equalsIgnoreCase(
								"EXTENSIONS")) {
							obs.getText().setStatus(
									NarrativeStatusEnum.EXTENSIONS);
						} else {
							obs.getText().setStatus(
									NarrativeStatusEnum.GENERATED);
						}
						obs.getText().setDiv(textBlock);

						retVal.add(obs);
					} else if (tableName.equals(CONDITION)) {
						String theId = rs.getString("ID");
						String nameUri = rs.getString("NAMEURI");
						String nameCode = rs.getString("NAMECODING");
						String nameDisp = rs.getString("NAMEDISPLAY");
						String subject = rs.getString("SUBJECT");
						String status = rs.getString("STATUS");
						Timestamp onsetTS = rs.getTimestamp("ONSET");
						java.util.Date onsetDate = null;
						if (onsetTS != null) {
							onsetDate = new Date(onsetTS.getTime());
						}
						Timestamp dateAssertedTS = rs
								.getTimestamp("DATEASSERTED");
						java.util.Date dateAssertedDate = null;
						if (dateAssertedTS != null) {
							dateAssertedDate = new Date(
									dateAssertedTS.getTime());
						}
						String textStatus = rs.getString("TEXTSTATUS");
						String narrative = rs.getString("NARRATIVE");

						Condition condition = new Condition();
						// Set ID
						condition.setId(theId);

						// Set subject reference to Patient
						ResourceReferenceDt subj = new ResourceReferenceDt(
								subject);
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
						if (status.equalsIgnoreCase("CONFIRMED"))
							condition.setStatus(ConditionStatusEnum.CONFIRMED);
						else if (status.equalsIgnoreCase("PROVISIONAL"))
							condition
									.setStatus(ConditionStatusEnum.PROVISIONAL);
						else if (status.equalsIgnoreCase("REFUTED"))
							condition.setStatus(ConditionStatusEnum.REFUTED);
						else if (status.equalsIgnoreCase("WORKING"))
							condition.setStatus(ConditionStatusEnum.WORKING);

						// Optional onsetdate
						// condition.setOnset(new DateTimeDt(dateTime));
						if (onsetDate != null)
							condition.setOnset(new DateDt(onsetDate));
						if (dateAssertedDate != null)
							condition.setDateAsserted(new DateDt(
									dateAssertedDate));

						// Optional human readable part.
						condition.getText().setDiv(narrative);

						if (textStatus.equalsIgnoreCase("GENERATED"))
							condition.getText().setStatus(
									NarrativeStatusEnum.GENERATED);
						else if (textStatus.equalsIgnoreCase("ADDITIONAL"))
							condition.getText().setStatus(
									NarrativeStatusEnum.ADDITIONAL);
						else if (textStatus.equalsIgnoreCase("EMPTY"))
							condition.getText().setStatus(
									NarrativeStatusEnum.EMPTY);
						else if (textStatus.equalsIgnoreCase("EXTENSIONS"))
							condition.getText().setStatus(
									NarrativeStatusEnum.EXTENSIONS);

						retVal.add(condition);
					} else if (tableName.equals(MEDICATIONPRESCRIPTION)) {
						String ID = rs.getString("ID");
						String nameUri = rs.getString("NAMEURI");
						String nameCode = rs.getString("NAMECODING");
						String nameDisp = rs.getString("NAMEDISPLAY");
						String patientRef = rs.getString("SUBJECT");
						String prescriberName = rs.getString("PRESCRIBER");
						String doseQty = rs.getString("DOSEQTY");
						String doseUnit = rs.getString("DOSEUNIT");
						String medSig = rs.getString("DOSAGEINSTRUCTION");
						double medQty = (double) rs.getInt("DISPENSEQTY");
						int medRefill = rs.getInt("REFILL");
						Timestamp dateWrittenTS = rs
								.getTimestamp("DATEWRITTEN");
						java.util.Date dateWrittenDate = null;
						if (dateWrittenTS != null) {
							dateWrittenDate = new Date(dateWrittenTS.getTime());
						}
						String status = rs.getString("STATUS");
						String textStatus = rs.getString("TEXTSTATUS");
						String textBlock = rs.getString("NARRATIVE");

						MedicationPrescription medicationPrescript = new MedicationPrescription();
						medicationPrescript.setId(ID);

						// Set Patient whom this prescription is for
						ResourceReferenceDt patientRefDt = new ResourceReferenceDt(
								patientRef);
						medicationPrescript.setPatient(patientRefDt);

						// Prescriber. We don't have Prescriber resource yet.
						// Just display it.
						ResourceReferenceDt providerRefDt = new ResourceReferenceDt();
						providerRefDt.setDisplay(prescriberName);
						medicationPrescript.setPrescriber(providerRefDt);

						// Medication information. We do not have the medication
						// resource yet.
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
						medResource.setId("1");
						List<IResource> medResList = new ArrayList<IResource>();
						medResList.add(medResource);
						ContainedDt medContainedDt = new ContainedDt();
						medContainedDt.setContainedResources(medResList);
						medicationPrescript.setContained(medContainedDt);

						// Medication reference. This should point to the
						// contained resource.
						ResourceReferenceDt medRefDt = new ResourceReferenceDt(
								"#" + 1);
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
							medDosageInstruct
									.setText("Dose Quanty: " + doseQty);
						}
						medDosageInstruct.setText(medSig);
						ArrayList<DosageInstruction> medDoseInstList = new ArrayList<DosageInstruction>();
						medDoseInstList.add(medDosageInstruct);
						medicationPrescript
								.setDosageInstruction(medDoseInstList);

						// Dispense Qty and Refill
						Dispense medDispense = new Dispense();
						medDispense.setQuantity(new QuantityDt(medQty));
						medDispense.setNumberOfRepeatsAllowed(medRefill);
						medicationPrescript.setDispense(medDispense);

						// Date Written
						medicationPrescript.setDateWritten(new DateTimeDt(
								dateWrittenDate));

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

						// Optional human readable part.
						if (textBlock != null && !textBlock.isEmpty()) {
							medicationPrescript.getText().setDiv(textBlock);
							if (textStatus.equalsIgnoreCase("GENERATED"))
								medicationPrescript.getText().setStatus(
										NarrativeStatusEnum.GENERATED);
							else if (textStatus.equalsIgnoreCase("ADDITIONAL"))
								medicationPrescript.getText().setStatus(
										NarrativeStatusEnum.ADDITIONAL);
							else if (textStatus.equalsIgnoreCase("EMPTY"))
								medicationPrescript.getText().setStatus(
										NarrativeStatusEnum.EMPTY);
							else if (textStatus.equalsIgnoreCase("EXTENSIONS"))
								medicationPrescript.getText().setStatus(
										NarrativeStatusEnum.EXTENSIONS);
						}
						retVal.add(medicationPrescript);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			connection.close();
		}

		return retVal;
	}

	public static String findIdFromTag(String tag) throws SQLException {
		// read organization id that this tag is assigned to.

		String retVal = null;
		Connection connection = null;
		Statement statement = null;
		try {
			DataSource tempds = (DataSource) new InitialContext()
					.lookup("java:/comp/env/jdbc/HealthPort");

			connection = tempds.getConnection();
			statement = connection.createStatement();
			String SQL_STATEMENT = "SELECT * FROM ORGANIZATION WHERE TAG='"
					+ tag + "'";
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			if (resultSet.next()) {
				retVal = resultSet.getString("ID");
			}

		} catch (NamingException | SQLException e) {
			e.printStackTrace();
		} finally {
			connection.close();
		}
		return retVal;
	}

	private void databaseSetup(String jndiName) {
		try {
			dataSource = (DataSource) new InitialContext()
					.lookup("java:/comp/env/" + jndiName);
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}

	public Connection getConnection() {
		try {
			return dataSource.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void resetInformation() {
		userId = null;
		name = null;
		recordId = null;
		personId = null;
		source = null;
		gender = null;
		contact = null;
		address = null;
	}

	public void setRSInformation(ResultSet rs) throws SQLException {
		userId = rs.getString("ID");
		name = rs.getString("NAME");
		source = rs.getString("TAG");
		orgID = rs.getString("ORGANIZATIONID");
		recordId = rs.getString("RECORDID");
		personId = rs.getString("PERSONID");
		gender = rs.getString("GENDER");
		contact = rs.getString("CONTACT");
		address = rs.getString("ADDRESS");
	}

	public void setInformation(String userId) {
		Connection connection = null;
		Statement statement = null;
		// Context context = null;
		// DataSource datasource = null;

		this.userId = userId;

		try {
			connection = getConnection();
			statement = connection.createStatement();
			String SQL_STATEMENT = "SELECT U1.ID, U1.ORGANIZATIONID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID) WHERE U1.ID="
					+ userId;
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);

			if (resultSet.next()) {
				setRSInformation(resultSet);
				// System.out.println("[HealthPortUserInfo]"+userId);
				// System.out.println("[HealthPortUserInfo]"+name+":"+dataSource);
			}

		} catch (Exception e) {
			e.printStackTrace();
			resetInformation();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void setInformationPersonID(String Id, String OrgID) {
		Connection connection = null;
		Statement statement = null;
		// Context context = null;
		// DataSource datasource = null;

		this.personId = Id;

		try {
			connection = getConnection();
			statement = connection.createStatement();
			String SQL_STATEMENT = "SELECT U1.ID, U1.ORGANIZATIONID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID) WHERE U1.PERSONID='"
					+ Id + "' AND U1.ORGANIZATIONID='" + OrgID + "'";
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);

			if (resultSet.next()) {
				setRSInformation(resultSet);
				// System.out.println("[HealthPortUserInfo]"+userId);
				// System.out.println("[HealthPortUserInfo]"+name+":"+dataSource);
			}
		} catch (Exception e) {
			e.printStackTrace();
			resetInformation();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public String getOrgID(String Tag) {
		String orgID = null;
		Connection connection = getConnection();
		try {
			Statement statement = connection.createStatement();
			String SQL_STATEMENT = "SELECT * FROM ORGANIZATION WHERE TAG='"
					+ Tag + "'";
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);

			if (resultSet.next()) {
				orgID = resultSet.getString("ID");
				// System.out.println("[HealthPortUserInfo]"+userId);
				// System.out.println("[HealthPortUserInfo]"+name+":"+dataSource);
			}

			
		} catch (Exception e) {
			e.printStackTrace();
			resetInformation();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return orgID;
	}
}
