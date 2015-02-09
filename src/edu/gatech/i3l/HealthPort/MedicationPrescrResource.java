package edu.gatech.i3l.HealthPort;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.Medication;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;

public class MedicationPrescrResource implements IResourceProvider {
	public static final String SQL_STATEMENT = "SELECT U1.ID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID)";

	private SyntheticEHRPort syntheticEHRPort;
	private HealthVaultPort healthvaultPort;

	// Constructor
	public MedicationPrescrResource() {
		syntheticEHRPort = new SyntheticEHRPort();
		healthvaultPort = new HealthVaultPort();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */
	@Override
	public Class<? extends IResource> getResourceType() {
		// TODO Auto-generated method stub
		return MedicationPrescription.class;
	}

	@Read()
	public MedicationPrescription getResourceById(@IdParam IdDt theId) {
		MedicationPrescription med = null;
		String resourceId = theId.getIdPart();
		String[] Ids = theId.getIdPart().split("\\-", 3);

		HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(
				Integer.parseInt(Ids[0]));
		String location = HealthPortUser.dataSource;

		if (location.equals(HealthPortUserInfo.GREENWAY)) {
			System.out.println("Greenway");

		} else if (location.equals(HealthPortUserInfo.SyntheticEHR)) {
			// med = new
			// SyntheticEHRPort().getMedicationPrescription(resourceId);
			med = syntheticEHRPort.getMedicationPrescription(resourceId);

		} else if (location.equals(HealthPortUserInfo.HEALTHVAULT)) {
			// med = new
			// HealthVaultPort().getMedicationPrescription(resourceId);
			med = healthvaultPort.getMedicationPrescription(resourceId);
		}

		return med;
	}

	@Search
	public List<MedicationPrescription> getAllMedicationPrescriptions() {
		Connection connection = null;
		Statement statement = null;
		Context context = null;
		DataSource datasource = null;
		String ccd = null;

		ArrayList<MedicationPrescription> finalRetVal = new ArrayList<MedicationPrescription>();
		ArrayList<MedicationPrescription> retVal = null;
		try {
			context = new InitialContext();
			datasource = (DataSource) context
					.lookup("java:/comp/env/jdbc/HealthPort");
			connection = datasource.getConnection();
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			HealthPortUserInfo HealthPortUser = new HealthPortUserInfo();
			while (resultSet.next()) {
				HealthPortUser.userId = String.valueOf(resultSet.getInt("ID"));
				HealthPortUser.dataSource = resultSet.getString("TAG");
				HealthPortUser.recordId = resultSet.getString("RECORDID");
				HealthPortUser.personId = resultSet.getString("PERSONID");
				HealthPortUser.gender = resultSet.getString("GENDER");
				HealthPortUser.contact = resultSet.getString("CONTACT");
				HealthPortUser.address = resultSet.getString("ADDRESS");

				if (HealthPortUser.dataSource
						.equals(HealthPortUserInfo.GREENWAY)) {
					ccd = GreenwayPort.getCCD(HealthPortUser.personId);
					// System.out.println(ccd);
				} else if (HealthPortUser.dataSource
						.equals(HealthPortUserInfo.SyntheticEHR)) {
					retVal = syntheticEHRPort
							.getMedicationPrescriptions(HealthPortUser);
					if (retVal != null && !retVal.isEmpty()) {
						finalRetVal.addAll(retVal);
					}
				} else if (HealthPortUser.dataSource
						.equals(HealthPortUserInfo.HEALTHVAULT)) {
					retVal = healthvaultPort
							.getMedicationPrescriptions(HealthPortUser);
					if (retVal != null && !retVal.isEmpty()) {
						finalRetVal.addAll(retVal);
					}
				}

				retVal = null;

			}
			connection.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return finalRetVal;

	}

	@Search()
	public List<MedicationPrescription> getMedicationPrescriptionsByPatient(
			@RequiredParam(name = Condition.SP_SUBJECT) ReferenceParam theSubject) {

		ArrayList<MedicationPrescription> retVal = null;
		String ccd = null;
		int patientNum = Integer.parseInt(theSubject.getIdPart());
		HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(patientNum);

		if (HealthPortUser.dataSource.equals(HealthPortUserInfo.GREENWAY)) {
			ccd = GreenwayPort.getCCD(HealthPortUser.personId);
			System.out.println(ccd);
		} else if (HealthPortUser.dataSource
				.equals(HealthPortUserInfo.SyntheticEHR)) {
			// retVal = new
			// SyntheticEHRPort().getMedicationPrescriptions(HealthPortUser);
			retVal = syntheticEHRPort
					.getMedicationPrescriptions(HealthPortUser);

		} else if (HealthPortUser.dataSource
				.equals(HealthPortUserInfo.HEALTHVAULT)) {
			// retVal = new
			// HealthVaultPort().getMedicationPrescriptions(HealthPortUser);
			retVal = healthvaultPort.getMedicationPrescriptions(HealthPortUser);
		}

		return retVal;
	}

	@Search
	public List<MedicationPrescription> findMedPrescriptListWithChain(
			@RequiredParam(name = MedicationPrescription.SP_MEDICATION, chainWhitelist = { Medication.SP_NAME }) ReferenceParam theMedication) {
		// This is /MedicationPrescription?medication.name=<medicine_name>.
		// We are chaining "name" parameter in Medication resource to MedicationPrescription.
		
		List<MedicationPrescription> retVal = null;

		// The search parameter is medication in the MedicationPrescription resource
		// with chained search parameter, "name" in Medication resource. 
		// Check if the chained parameter search is "medication.name".
		String chain = theMedication.getChain();
		if (Medication.SP_NAME.equals(chain)) {
			String medName = theMedication.getValue();
			retVal = syntheticEHRPort.getMedicationPrescriptionsByType(medName);
		}

		return retVal;
	}

}