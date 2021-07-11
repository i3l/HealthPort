package edu.gatech.i3l.HealthPort.providers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.Medication;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import edu.gatech.i3l.HealthPort.ports.GreenwayPort;
import edu.gatech.i3l.HealthPort.HealthPortInfo;
import edu.gatech.i3l.HealthPort.ports.HealthVaultPort;
import edu.gatech.i3l.HealthPort.ports.ExactDataPort;

public class MedicationPrescrResource implements IResourceProvider {
//	private ExactDataPort syntheticEHRPort;
//	private HealthVaultPort healthvaultPort;
//	private GreenwayPort greenwayPort;
	private HealthPortInfo healthPortUser;
//	private ExactDataPort syntheticCancerPort;

	// Constructor
	public MedicationPrescrResource() {
//		syntheticEHRPort = new ExactDataPort("jdbc/ExactDataSample",
//				ExactDataPort.SyntheticEHR);
//		healthvaultPort = new HealthVaultPort();
//		greenwayPort = new GreenwayPort();
		healthPortUser = new HealthPortInfo("jdbc/HealthPort");
//		syntheticCancerPort = new ExactDataPort("jdbc/ExactDataCancer",
//				ExactDataPort.SyntheticCancer);
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
		List<String>Ids = new ArrayList<String>();
		Ids.add(theId.getIdPart());
		
		List<IResource> resourceList = null;
		try {
			resourceList = healthPortUser.getResourceList(HealthPortInfo.MEDICATIONPRESCRIPTION, Ids);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (resourceList == null || resourceList.isEmpty()) return null;
		return (MedicationPrescription) resourceList.get(0);		

		
		
//		MedicationPrescription med = null;
//		String[] Ids = theId.getIdPart().split("\\.", 2);
//
//		if (Ids[0].equals(greenwayPort.getId())) {
//			System.out.println("Greenway");
//		} else if (Ids[0].equals(healthvaultPort.getId())) {
//			med = healthvaultPort.getMedicationPrescription(Ids[1]);
//		} else if (Ids[0].equals(syntheticEHRPort.getId())) {
//			med = syntheticEHRPort.getMedicationPrescription(Ids[1]);
//		} else if (Ids[0].equals(syntheticCancerPort.getId())) {
//			med = syntheticCancerPort.getMedicationPrescription(Ids[1]);
//		}
//
//		return med;
	}

	@Search
	public IBundleProvider getAllMedicationPrescriptions() {
		final InstantDt searchTime = InstantDt.withCurrentTime();
		final List<String> matchingResourceIds = healthPortUser
				.getAllResourceIds(HealthPortInfo.MEDICATIONPRESCRIPTION);

		return new IBundleProvider() {

			@Override
			public int size() {
				return matchingResourceIds.size();
			}

			@Override
			public List<IResource> getResources(int theFromIndex, int theToIndex) {
				int end = Math.min(theToIndex, matchingResourceIds.size());
				// System.out.println("From:"+theFromIndex+" To:"+theToIndex+" Total:"+matchingResourceIds.size());
				List<String> idsToReturn = matchingResourceIds.subList(
						theFromIndex, end);
				List<IResource> retVal = null;
				try {
					retVal = healthPortUser.getResourceList(HealthPortInfo.MEDICATIONPRESCRIPTION,
							idsToReturn);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return retVal;
			}

			@Override
			public InstantDt getPublished() {
				return searchTime;
			}
		};
		
//		Connection connection = null;
//		Statement statement = null;
//		String ccd = null;
//
//		ArrayList<MedicationPrescription> finalRetVal = new ArrayList<MedicationPrescription>();
//		ArrayList<MedicationPrescription> retVal = null;
//		try {
//			connection = healthPortUser.getConnection();
//			statement = connection.createStatement();
//			String sql = "SELECT U1.ID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID)"
//					+ "WHERE ORG.TAG='" + GreenwayPort.GREENWAY + "' OR ORG.TAG='" + HealthVaultPort.HEALTHVAULT + "'";
//
//			ResultSet resultSet = statement.executeQuery(sql);
//			while (resultSet.next()) {
//				healthPortUser.setRSInformation(resultSet);
//
//				if (healthPortUser.orgID.equals(greenwayPort.getId())) {
//					ccd = GreenwayPort.getCCD(healthPortUser.personId);
//					// System.out.println(ccd);
//				} else if (healthPortUser.orgID
//						.equals(healthvaultPort.getId())) {
//					retVal = healthvaultPort
//							.getMedicationPrescriptions(healthPortUser);
//					if (retVal != null && !retVal.isEmpty()) {
//						finalRetVal.addAll(retVal);
//					}
//				}
//				retVal = null;
//
//			}
//
//			connection.close();
//
//			retVal = syntheticEHRPort.getMedicationPrescriptions();
//			if (retVal != null && !retVal.isEmpty()) {
//				finalRetVal.addAll(retVal);
//			}
//
//			retVal = syntheticCancerPort.getMedicationPrescriptions();
//			if (retVal != null && !retVal.isEmpty()) {
//				finalRetVal.addAll(retVal);
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return finalRetVal;

	}

	@Search()
	public IBundleProvider getMedicationPrescriptionsByPatient(
			@RequiredParam(name = MedicationPrescription.SP_PATIENT) ReferenceParam theSubject) {
		final InstantDt searchTime = InstantDt.withCurrentTime();
		String patientID = theSubject.getIdPart();

		final List<String> matchingResourceIds = healthPortUser.getResourceIdsByPatient(HealthPortInfo.MEDICATIONPRESCRIPTION, patientID);

		return new IBundleProvider() {

			@Override
			public int size() {
				return matchingResourceIds.size();
			}

			@Override
			public List<IResource> getResources(int theFromIndex, int theToIndex) {
				int end = Math.min(theToIndex, matchingResourceIds.size());
				// System.out.println("From:"+theFromIndex+" To:"+theToIndex+" Total:"+matchingResourceIds.size());
				List<String> idsToReturn = matchingResourceIds.subList(
						theFromIndex, end);
				
				List<IResource> retVal = null;
				try {
					retVal = healthPortUser.getResourceList(HealthPortInfo.MEDICATIONPRESCRIPTION,
							idsToReturn);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return retVal;
			}

			@Override
			public InstantDt getPublished() {
				return searchTime;
			}
		};
//		ArrayList<MedicationPrescription> retVal = null;
//		String ccd = null;
//
//		String Ids[] = theSubject.getIdPart().split("\\.", 2);
//
//		if (Ids[0].equals(greenwayPort.getId())
//				|| Ids[0].equals(healthvaultPort.getId())) {
//			healthPortUser.setInformation(Ids[1]);
//			if (healthPortUser.orgID != null) {
//				if (healthPortUser.orgID.equals(greenwayPort.getId())) {
//					ccd = GreenwayPort.getCCD(healthPortUser.personId);
//					// System.out.println(ccd);
//				} else if (healthPortUser.orgID
//						.equals(healthvaultPort.getId())) {
//					retVal = healthvaultPort
//							.getMedicationPrescriptions(healthPortUser);
//				}
//			}
//
//		} else {
//
//			if (Ids[0].equals(ExactDataPort.SyntheticEHR)) {
//				retVal = syntheticEHRPort
//						.getMedicationPrescriptionByPatient(Ids[1]);
//
//			} else if (Ids[0]
//					.equalsIgnoreCase(ExactDataPort.SyntheticCancer)) {
//				retVal = syntheticCancerPort
//						.getMedicationPrescriptionByPatient(Ids[1]);
//
//			}
//		}
//
//		return retVal;
	}

	@Search
	public IBundleProvider findMedPrescriptListWithChain(
			@RequiredParam(name = MedicationPrescription.SP_MEDICATION, chainWhitelist = { Medication.SP_NAME }) ReferenceParam theMedication) {
		
		final InstantDt searchTime = InstantDt.withCurrentTime();
		final List<String> matchingResourceIds = new ArrayList<String>();
		
		// This is /MedicationPrescription?medication.name=<medicine_name>.
		// We are chaining "name" parameter in Medication resource to
		// MedicationPrescription.
		String chain = theMedication.getChain();
		if (Medication.SP_NAME.equals(chain)) {
			String medName = theMedication.getValue();
			Connection conn = healthPortUser.getConnection();
			String SQL_STATEMENT = "SELECT * FROM "+HealthPortInfo.MEDICATIONPRESCRIPTION + " WHERE NAMEDISPLAY LIKE '%"+medName+"%'";
			try {
				PreparedStatement pmst = conn.prepareStatement(SQL_STATEMENT);
				ResultSet rs = pmst.executeQuery();

				while (rs.next()) {
					matchingResourceIds.add(rs.getString("ID"));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return new IBundleProvider() {

			@Override
			public int size() {
				return matchingResourceIds.size();
			}

			@Override
			public List<IResource> getResources(int theFromIndex, int theToIndex) {
				int end = Math.min(theToIndex, matchingResourceIds.size());
				// System.out.println("From:"+theFromIndex+" To:"+theToIndex+" Total:"+matchingResourceIds.size());
				List<String> idsToReturn = matchingResourceIds.subList(
						theFromIndex, end);
				
				List<IResource> retVal = null;
				try {
					retVal = healthPortUser.getResourceList(HealthPortInfo.MEDICATIONPRESCRIPTION,
							idsToReturn);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return retVal;
			}

			@Override
			public InstantDt getPublished() {
				return searchTime;
			}
		};
//		List<MedicationPrescription> retVal = new ArrayList<MedicationPrescription>();
//		List<MedicationPrescription> portRet = null;
//
//		// The search parameter is medication in the MedicationPrescription
//		// resource
//		// with chained search parameter, "name" in Medication resource.
//		// Check if the chained parameter search is "medication.name".
//		String chain = theMedication.getChain();
//		if (Medication.SP_NAME.equals(chain)) {
//			String medName = theMedication.getValue();
//			portRet = syntheticEHRPort
//					.getMedicationPrescriptionsByType(medName);
//			if (portRet != null && !portRet.isEmpty()) {
//				retVal.addAll(portRet);
//			}
//
//			portRet = syntheticCancerPort
//					.getMedicationPrescriptionsByType(medName);
//			if (portRet != null && !portRet.isEmpty()) {
//				retVal.addAll(portRet);
//			}
//		}
//
//		return retVal;
	}

}