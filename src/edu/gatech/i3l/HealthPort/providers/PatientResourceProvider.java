package edu.gatech.i3l.HealthPort.providers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.dstu.resource.Patient;
import ca.uhn.fhir.model.dstu.valueset.AdministrativeGenderCodesEnum;
import ca.uhn.fhir.model.dstu.valueset.MaritalStatusCodesEnum;
import ca.uhn.fhir.model.dstu.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import edu.gatech.i3l.HealthPort.HealthPortInfo;
import edu.gatech.i3l.HealthPort.ports.GreenwayPort;
import edu.gatech.i3l.HealthPort.ports.HealthVaultPort;

/**
 * All resource providers must implement IResourceProvider
 */
public class PatientResourceProvider implements IResourceProvider {
	public static final String SQL_STATEMENT = "SELECT U1.ID, U1.ORGANIZATIONID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS, U1.DOB, U1.ETHNICITY, U1.MARITAL_STATUS, U1.BLOOD_TYPE, U1.JOB, U1.ADVANCE_DIRECTIVE, U1.ORGAN_DONOR FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID);";

	private HealthPortInfo healthPortUser;
	private String GWID;
	private String HVID;

	public PatientResourceProvider() {
		healthPortUser = new HealthPortInfo("jdbc/HealthPort");
		GWID = healthPortUser.getOrgID(GreenwayPort.GREENWAY);
		HVID = healthPortUser.getOrgID(HealthVaultPort.HEALTHVAULT);
	}

	/**
	 * The getResourceType method comes from IResourceProvider, and must be
	 * overridden to indicate what type of resource this provider supplies.
	 */
	@Override
	public Class<Patient> getResourceType() {
		return Patient.class;
	}

	@Read()
	public Patient getResourceById(@IdParam IdDt theId) {
		Patient patient = new Patient();
		String Ids[] = theId.getIdPart().split("\\.", 2);

		// Verify ID token length
		if (Ids.length != 2) {
			return patient;
		}
		if (Ids[0].equals(GWID) || Ids[0].equals(HVID)) {
			// Search for patient record by ID
			healthPortUser.setInformation(Ids[1]);
		} else {
			// Search for patient record by ID & Org ID
			healthPortUser.setInformationPersonID(Ids[1], Ids[0]);
		}

		// Verify successful patient search
		if (healthPortUser.name.isEmpty()) {
			return null;
		}

		// Process Java object into FHIR Patient Resource object
		patient = buildPatient(healthPortUser);

		return patient;
	}

	@Search
	public List<Patient> getAllPatients() {
		// System.out.println("HERE");
		Connection connection = null;
		Statement statement = null;

		ArrayList<Patient> retVal = new ArrayList<Patient>();
		try {
			connection = healthPortUser.getConnection();
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			while (resultSet.next()) {
				// Process Result Set object into Java object
				healthPortUser.setRSInformation(resultSet);
				// Process Java object into FHIR Patient Resource object
				Patient patient = buildPatient(healthPortUser);
				// Prepare to return Patient
				retVal.add(patient);
			}
			connection.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return retVal;

	}

	/**
	 * Transform a HealthPortInfo object into a Patient object.
	 * 
	 * @author Chris Harmoney
	 * @since 2015-04-13
	 * @param healthPortUser
	 *            patient record to build
	 * @return Patient representing the argument HealthPortInfo
	 */
	private Patient buildPatient(HealthPortInfo healthPortUser) {
		Patient patient = new Patient();
		String[] userName = healthPortUser.name.split(" ");

		// Patient ID
		patient.addIdentifier();
		patient.getIdentifier().get(0)
				.setSystem(new UriDt("urn:healthport:mrns"));
		if (healthPortUser.orgID.equals(GWID)
				|| healthPortUser.orgID.equalsIgnoreCase(HVID)) {
			patient.setId(healthPortUser.orgID + "." + healthPortUser.userId);
			patient.getIdentifier()
					.get(0)
					.setValue(
							healthPortUser.orgID + "." + healthPortUser.userId);
		} else {
			patient.setId(healthPortUser.orgID + "." + healthPortUser.personId);
			patient.getIdentifier()
					.get(0)
					.setValue(
							healthPortUser.orgID + "."
									+ healthPortUser.personId);
		}

		// Name
		String fullName = null;
		if (userName.length == 2) {
			patient.addName().addFamily(userName[1]);
			patient.getName().get(0).addGiven(userName[0]);
			fullName = userName[0] + " " + userName[1];
		} else {
			patient.addName().addFamily(userName[2]);
			patient.getName().get(0).addGiven(userName[0] + " " + userName[1]);
			fullName = userName[0] + " " + userName[1] + " " + userName[2];
		}

		// Gender
		if (null == healthPortUser.gender) {
			// Null check: Unknown gender
			patient.setGender(AdministrativeGenderCodesEnum.UN);
		} else if (healthPortUser.gender.equalsIgnoreCase("female")) {
			patient.setGender(AdministrativeGenderCodesEnum.F);
		} else if (healthPortUser.gender.equalsIgnoreCase("male")) {
			patient.setGender(AdministrativeGenderCodesEnum.M);
		} else {
			// Unknown gender for all other values
			patient.setGender(AdministrativeGenderCodesEnum.UN);
		}

		// Date of Birth
		if (null != healthPortUser.dob) {
			// Sample data only includes DAY precision, not times
			patient.setBirthDate(healthPortUser.dob, TemporalPrecisionEnum.DAY);
		}

		// Marital Status
		// http://javadox.com/ca.uhn.hapi.fhir/hapi-fhir-base/0.4/ca/uhn/fhir/model/dstu/valueset/MaritalStatusCodesEnum.html
		// http://hl7.org/fhir/vs/marital-status
		// Code Display Definition
		// A Annulled Marriage contract has been declared null and to not have
		// existed
		// D Divorced Marriage contract has been declared dissolved and inactive
		// I Interlocutory Subject to an Interlocutory Decree.
		// L Legally Separated
		// M Married A current marriage contract is active
		// P Polygamous More than 1 current spouse
		// S Never Married No marriage contract has ever been entered
		// T Domestic partner Person declares that a domestic partner
		// relationship exists.
		// W Widowed The spouse has died
		if (null == healthPortUser.maritalStatus) {
			// Null check: Unknown marital status
			patient.setMaritalStatus(MaritalStatusCodesEnum.UNK);
		} else if (healthPortUser.maritalStatus.equalsIgnoreCase("Annulled")) {
			// Value not present in sample data, exact String is a guess
			patient.setMaritalStatus(MaritalStatusCodesEnum.A);
		} else if (healthPortUser.maritalStatus.equalsIgnoreCase("Divorced")) {
			patient.setMaritalStatus(MaritalStatusCodesEnum.D);
		} else if (healthPortUser.maritalStatus
				.equalsIgnoreCase("Interlocutory")) {
			// Value not present in sample data, exact String is a guess
			patient.setMaritalStatus(MaritalStatusCodesEnum.I);
		} else if (healthPortUser.maritalStatus
				.equalsIgnoreCase("Legally Separated")) {
			patient.setMaritalStatus(MaritalStatusCodesEnum.L);
		} else if (healthPortUser.maritalStatus.equalsIgnoreCase("Married")) {
			patient.setMaritalStatus(MaritalStatusCodesEnum.M);
		} else if (healthPortUser.maritalStatus.equalsIgnoreCase("Polygamous")) {
			// Value not present in sample data, exact String is a guess
			patient.setMaritalStatus(MaritalStatusCodesEnum.P);
		} else if (healthPortUser.maritalStatus.equalsIgnoreCase("Single")) {
			patient.setMaritalStatus(MaritalStatusCodesEnum.S);
		} else if (healthPortUser.maritalStatus
				.equalsIgnoreCase("Domestic partner")) {
			// Value not present in sample data, exact String is a guess
			patient.setMaritalStatus(MaritalStatusCodesEnum.T);
		} else if (healthPortUser.maritalStatus.equalsIgnoreCase("Widowed")) {
			patient.setMaritalStatus(MaritalStatusCodesEnum.W);
		} else {
			// Unknown otherwise
			patient.setMaritalStatus(MaritalStatusCodesEnum.UNK);
		}

		// Ethnicity not supported by
		// ca.uhn.fhir.model.dstu.resource.Patient
		// if (null != healthPortUser.ethnicity) {}

		// Blood Type not supported by
		// ca.uhn.fhir.model.dstu.resource.Patient
		// if (null != healthPortUser.bloodType) {}

		// Job not supported by
		// ca.uhn.fhir.model.dstu.resource.Patient
		// if (null != healthPortUser.job) {}

		// Advance Directive not supported by
		// ca.uhn.fhir.model.dstu.resource.Patient
		// if (null != healthPortUser.advanceDirective) {}

		// Organ Donor not supported by
		// ca.uhn.fhir.model.dstu.resource.Patient
		// if (null != healthPortUser.organDonor) {}

		// HTML Representation
		patient.getText().setStatus(NarrativeStatusEnum.GENERATED);
		StringBuilder sb = new StringBuilder(
				"<table><tr><th>Name</th><th>DOB</th><th>Gender</th><th>Marital Status</th></tr><tr><td>");
		sb.append(fullName);
		sb.append("</td><td>");
		if (null != patient.getBirthDate()) {
			sb.append(patient.getBirthDate().getValueAsString());
		}
		sb.append("</td><td>");
		sb.append(Objects.toString(healthPortUser.gender, ""));
		sb.append("</td><td>");
		sb.append(Objects.toString(healthPortUser.maritalStatus, ""));
		sb.append("</td></tr></table>");
		patient.getText().setDiv(sb.toString());

		return patient;
	}
}