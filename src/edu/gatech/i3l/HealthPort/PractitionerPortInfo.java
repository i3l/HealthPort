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
import edu.gatech.i3l.HealthPort.providers.PatientResourceProvider;

/**
 * @author Myung Choi Constructor requires UserID. - Connects to SQL database to
 *         obtain patient related parameters.
 */
public class PractitionerPortInfo {


	public String	 providerOrg              = null;
	public String    providerId               = null;
	public String    providerNpi              = null;
	public String    facilityName             = null;
	public String    specialty                 = null;
	public String    name                      = null;
	public String    pClass                     = null;
	public String    location                  = null;
	public Date    	 dateOfBirth                = null;
	public String    sex                       = null;
	public String    supervisionRequired      = null;
	public String    personId                 = null;
	public String    primaryClinicLocation   = null;

	// Database Paging Resource Names
	public static String OBSERVATION = "OBSERVATION";
	public static String CONDITION = "CONDITIONS";
	public static String MEDICATIONPRESCRIPTION = "MEDICATIONPRESCRIPTION";

	private DataSource dataSource;

	/**
	 * 
	 */
	public PractitionerPortInfo() {
		databaseSetup("jdbc/HealthPort");
	}

	public PractitionerPortInfo(String jndiName) {
		databaseSetup(jndiName);
	}

	public PractitionerPortInfo(String jndiName, String userId) {
		databaseSetup(jndiName);
		setInformation(userId);
	}

	public List<String> getResourceIdsByProviderName(String tableName,
			String practitionerName) {
		List<String> retVal = new ArrayList<String>();

		Connection connection = null;
		Statement statement = null;

		String SQL_STATEMENT = "SELECT Provider_ID FROM " + tableName
				+ " WHERE name = '" + practitionerName + "'";

		System.out.println ("HealthPortInfo: getResourceIdsByProviders: "+tableName+" for Provider "+practitionerName);
		connection = getConnection();
		try {
			statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(SQL_STATEMENT);

			while (rs.next()) {
				retVal.add(rs.getString("Provider_ID"));
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

		System.out.println ("HealthPortInfo: getResourceIdsByProviders: Done");

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
		
		providerOrg              = null;
		providerId               = null;
		providerNpi              = null;
		facilityName             = null;
		specialty                 = null;
		name                      = null;
		pClass                     = null;
		location                  = null;
		dateOfBirth                = null;
		sex                       = null;
		supervisionRequired      = null;
		personId                 = null;
		primaryClinicLocation   = null;

	}

	public void setRSInformation(ResultSet rs) throws SQLException {


		providerOrg              = rs.getString("Provider_Org");
		providerId               = rs.getString("Provider_ID");
		providerNpi              = rs.getString("Provider_NPI");
		facilityName             = rs.getString("Facility_Name");
		specialty                 = rs.getString("Specialty");
		name                      = rs.getString("Name");
		pClass                     = rs.getString("Class");
		location                  = rs.getString("Location");
		sex                       = rs.getString("Sex");
		supervisionRequired      = rs.getString("Supervision_Required");
		personId                 = rs.getString("Person_ID");
		primaryClinicLocation   = rs.getString("Primary_Clinic_Location");

/*		System.out.println("Provider_Org="+ rs.getString("Provider_Org"));
		System.out.println("Provider_ID=" + rs.getString("Provider_ID"));
		System.out.println("Provider_NPI=" + rs.getString("Provider_NPI"));
		System.out.println("Facility_Name=" + rs.getString("Facility_Name"));
		System.out.println("Specialty=" + rs.getString("Specialty"));
		System.out.println("Name=" + rs.getString("Name"));
		System.out.println("Class=" + rs.getString("Class"));
		System.out.println("Location=" + rs.getString("Location"));
		System.out.println("Sex=" + rs.getString("Sex"));
		System.out.println("Supervision_Required=" + rs.getString("Supervision_Required"));
		System.out.println("Person_ID="+ rs.getString("Person_ID"));
		System.out.println("Primary_Clinic_Location=" + rs.getString("Primary_Clinic_Location"));


*/
	}

	public void setInformation(String providerId) {

		Connection connection = null;
		Statement statement = null;

		this.providerId = providerId;

		try {
			connection = getConnection();
			statement = connection.createStatement();
			String SQL_STATEMENT = "SELECT Provider_ID, Provider_Org, Provider_NPI, Facility_Name, Specialty, Name, Class, Location, Sex, Supervision_Required, Person_ID, Primary_Clinic_Location FROM provider where Provider_ID='" + providerId + "'";

			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);

			if(resultSet!= null) {
				if (resultSet.next()) {
					setRSInformation(resultSet);
				}
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

	/**
	 * @param string
	 * @param string2
	 */
	public void setInformationProviderID(String string, String string2) {
		// TODO Auto-generated method stub
		
	}
	

}
