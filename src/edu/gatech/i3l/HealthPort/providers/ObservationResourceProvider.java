package edu.gatech.i3l.HealthPort.providers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import edu.gatech.i3l.HealthPort.ports.GreenwayPort;
import edu.gatech.i3l.HealthPort.HealthPortInfo;
import edu.gatech.i3l.HealthPort.ports.HealthVaultPort;
import edu.gatech.i3l.HealthPort.ports.ExactDataPort;

public class ObservationResourceProvider implements IResourceProvider {
	private ExactDataPort syntheticEHRPort;
	private HealthVaultPort healthvaultPort;
	private GreenwayPort greenwayPort;
	private HealthPortInfo healthPortUser;
	private ExactDataPort syntheticCancerPort;

	// Constructor
	public ObservationResourceProvider() {
		syntheticEHRPort = new ExactDataPort("jdbc/ExactDataSample",
				ExactDataPort.SyntheticEHR);
		healthvaultPort = new HealthVaultPort();
		greenwayPort = new GreenwayPort();
		healthPortUser = new HealthPortInfo("jdbc/HealthPort");
		syntheticCancerPort = new ExactDataPort("jdbc/ExactDataCancer",
				ExactDataPort.SyntheticCancer);
	}

	/**
	 * The getResourceType method comes from IResourceProvider, and must be
	 * overridden to indicate what type of resource this provider supplies.
	 */
	public Class<Observation> getResourceType() {
		return Observation.class;
	}

	@Read()
	public Observation getResourceById(@IdParam IdDt theId) {
		List<String> Ids = new ArrayList<String>();
		Ids.add(theId.getIdPart());

		List<IResource> resourceList = null;
		try {
			resourceList = healthPortUser.getResourceList(
					HealthPortInfo.OBSERVATION, Ids);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (resourceList == null || resourceList.isEmpty())
			return null;
		return (Observation) resourceList.get(0);

		// // System.out.println(location);
		// if (Ids[0].equals(greenwayPort.getId())) {
		// System.out.println("Greenway");
		// } else if (Ids[0].equals(syntheticEHRPort.getId())) {
		// obs = syntheticEHRPort.getObservation(Ids[1]);
		// } else if (Ids[0].equals(healthvaultPort.getId())) {
		// obs = healthvaultPort.getObservation(Ids[1]);
		// } else if (Ids[0].equals(syntheticCancerPort.getId())) {
		// obs = syntheticCancerPort.getObservation(Ids[1]);
		// }
		//
		// return obs;
	}

	@Search
	// public List<Observation> getAllObservations() {
	public IBundleProvider getAllObservations() {
		final InstantDt searchTime = InstantDt.withCurrentTime();
		final List<String> matchingResourceIds = healthPortUser
				.getAllResourceIds(HealthPortInfo.OBSERVATION);

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
					retVal = healthPortUser.getResourceList(
							HealthPortInfo.OBSERVATION, idsToReturn);
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
	}

	// private List<String> getAllObsIds() {
	// List<String> finalRetVal = new ArrayList<String>();
	//
	// Connection connection = null;
	// Statement statement = null;
	//
	// String SQL_STATEMENT = "SELECT ID FROM OBSERVATION";
	//
	// connection = healthPortUser.getConnection();
	// try {
	// statement = connection.createStatement();
	// ResultSet rs = statement.executeQuery(SQL_STATEMENT);
	//
	// while (rs.next()) {
	// finalRetVal.add(rs.getString("ID"));
	// }
	// } catch (SQLException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// return finalRetVal;
	// }

	// private List<IResource> loadResourcesByIds(List<String> Ids) {
	// List<IResource> retVal = new ArrayList<IResource>();
	//
	// try {
	// retVal = healthPortUser.getResourceList(HealthPortInfo.OBSERVATION,
	// Ids);
	// } catch (SQLException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// return retVal;
	// }

	@Search()
	// public List<Observation> getObservationsbyPatient(
	public IBundleProvider getObservationsbyPatient(
			@RequiredParam(name = Observation.SP_SUBJECT) ReferenceParam theSubject) {

		final InstantDt searchTime = InstantDt.withCurrentTime();
		String patientID;

		if (theSubject.hasResourceType()) {
			String resourceType = theSubject.getResourceType();
			if ("Patient".equals(resourceType) == false) {
				throw new InvalidRequestException(
						"Invalid resource type for parameter 'subject': "
								+ resourceType);
			} else {
				patientID = theSubject.getIdPart();
			}
		} else {
			throw new InvalidRequestException(
					"Need resource type for parameter 'subject'");
		}

		final List<String> matchingResourceIds = healthPortUser
				.getResourceIdsByPatient(HealthPortInfo.OBSERVATION, patientID);

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
					retVal = healthPortUser.getResourceList(
							HealthPortInfo.OBSERVATION, idsToReturn);
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

		// String ccd = null;
		//
		// ArrayList<Observation> retVal = null;
		// String PatientID;
		//
		// if (theSubject.hasResourceType()) {
		// String resourceType = theSubject.getResourceType();
		// if ("Patient".equals(resourceType) == false) {
		// throw new InvalidRequestException(
		// "Invalid resource type for parameter 'subject': "
		// + resourceType);
		// } else {
		// PatientID = theSubject.getIdPart();
		// }
		// } else {
		// throw new InvalidRequestException(
		// "Need resource type for parameter 'subject'");
		// }
		//
		// String Ids[] = PatientID.split("\\.", 2);
		// try {
		//
		// if (Ids[0].equals(greenwayPort.getId())
		// || Ids[0].equals(healthvaultPort.getId())) {
		// healthPortUser.setInformation(Ids[1]);
		// if (healthPortUser.source == null)
		// return retVal;
		//
		// if (healthPortUser.orgID.equals(greenwayPort.getId())) {
		// ccd = GreenwayPort.getCCD(healthPortUser.personId);
		// // System.out.println(ccd);
		// } else if (healthPortUser.orgID.equals(healthvaultPort.getId())) {
		// retVal = healthvaultPort.getObservations(healthPortUser);
		// }
		//
		// } else if (Ids[0].equals(syntheticEHRPort.getId())) {
		// retVal = syntheticEHRPort.getObservationByPatient(Ids[1]);
		//
		// } else if (Ids[0].equals(syntheticCancerPort.getId())) {
		// retVal = syntheticCancerPort.getObservationByPatient(Ids[1]);
		// }
		// } catch (SQLException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// return retVal;
	}

	List<String> getObsIdsByPatient(String patientId) {
		List<String> finalRetVal = new ArrayList<String>();

		Connection connection = null;
		Statement statement = null;

		String SQL_STATEMENT = "SELECT ID FROM OBSERVATION WHERE SUBJECT = 'Patient/"
				+ patientId + "'";

		connection = healthPortUser.getConnection();
		try {
			statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(SQL_STATEMENT);

			while (rs.next()) {
				finalRetVal.add(rs.getString("ID"));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return finalRetVal;
	}

	@Search()
	public IBundleProvider searchByName(
			@RequiredParam(name = Observation.SP_NAME) TokenParam theName) {
		// This is search by Observation.name - codeableconcept.
		// Observation in FHIR supports LOINC. And, the data we have in the
		// Synthetic EHR has only LOINC in observation data.

		final InstantDt searchTime = InstantDt.withCurrentTime();

		String systemName = theName.getSystem();
		String codeName = theName.getValue();
		
		if (systemName != null
				&& !systemName.isEmpty()
				&& !systemName.equalsIgnoreCase("http://loinc.org")
				&& !systemName
						.equalsIgnoreCase("urn:oid:2.16.840.1.113883.6.1")) {
			// SyntheticEHR only has LOINC code name for observation data. But,
			// pass system name anyway just in case.
			return null;
		}

		final List<String> matchingResourceIds = healthPortUser.getResourceIdsByCodeSystem(HealthPortInfo.OBSERVATION, systemName, codeName);

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
					retVal = healthPortUser.getResourceList(
							HealthPortInfo.OBSERVATION, idsToReturn);
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

		// List<Observation> portRet = null;
		//
		// portRet = syntheticEHRPort.getObservationsByCodeSystem(systemName,
		// codeName);
		// if (portRet != null && !portRet.isEmpty()) {
		// retVal.addAll(portRet);
		// }
		//
		// portRet = syntheticCancerPort.getObservationsByCodeSystem(systemName,
		// codeName);
		// if (portRet != null && !portRet.isEmpty()) {
		// retVal.addAll(portRet);
		// }
		//
		// return retVal;
	}
}