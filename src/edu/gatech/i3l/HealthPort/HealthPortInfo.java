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

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu.composite.CodingDt;
import ca.uhn.fhir.model.dstu.composite.QuantityDt;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationReliabilityEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationStatusEnum;
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
	public static String CONDITION = "CONDITION";
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

	public static void storeResource(String tableName,
			ObservationSerializable obj) throws SQLException {
		Connection connection = null;
		String SQL_STATEMENT;

		try {
			DataSource ds = (DataSource) new InitialContext()
					.lookup("java:/comp/env/jdbc/HealthPort");

			connection = ds.getConnection();
			PreparedStatement pstmt = null;
			if (tableName.equals(OBSERVATION)) {
				SQL_STATEMENT = "REPLACE INTO "
						+ tableName
						+ " (ID, NAMEURI, NAMECODING, NAMEDISPLAY, QUANTITY, UNIT, COMMENT, SUBJECT, STATUS, RELIABILITY, APPLIES, ISSUED, TEXTSTATUS, NARRATIVE) VALUES "
						+ " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				pstmt = connection.prepareStatement(SQL_STATEMENT);

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
			} else {
				return;
			}
			pstmt.executeUpdate();
			pstmt.close();
		} catch (NamingException | SQLException e) {
			e.printStackTrace();
		} finally {
			connection.close();
		}
	}

	public static List<IResource> getResourceList(String tableName,
			List<String> Ids) throws SQLException {
		List<IResource> retVal = new ArrayList<IResource>();
		Connection connection = null;
		Statement getRes = null;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

		try {
			DataSource ds = (DataSource) new InitialContext()
					.lookup("java:/comp/env/jdbc/HealthPort");

			connection = ds.getConnection();
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
					}
				}
			}
		} catch (NamingException | SQLException e) {
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
			;
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

			connection.close();

		} catch (Exception e) {
			e.printStackTrace();
			resetInformation();
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

			connection.close();

		} catch (Exception e) {
			e.printStackTrace();
			resetInformation();
		}
	}

	public String getOrgID(String Tag) {
		String orgID = null;

		try {
			Connection connection = getConnection();
			Statement statement = connection.createStatement();
			String SQL_STATEMENT = "SELECT * FROM ORGANIZATION WHERE TAG='"
					+ Tag + "'";
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);

			if (resultSet.next()) {
				orgID = resultSet.getString("ID");
				// System.out.println("[HealthPortUserInfo]"+userId);
				// System.out.println("[HealthPortUserInfo]"+name+":"+dataSource);
			}

			connection.close();

		} catch (Exception e) {
			e.printStackTrace();
			resetInformation();
		}

		return orgID;
	}
}
