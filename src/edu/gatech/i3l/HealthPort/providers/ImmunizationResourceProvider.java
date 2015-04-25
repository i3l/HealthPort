package edu.gatech.i3l.HealthPort.providers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Immunization;
import ca.uhn.fhir.model.dstu.resource.Condition;
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
import edu.gatech.i3l.HealthPort.HealthPortInfo;
import edu.gatech.i3l.HealthPort.ports.GreenwayPort;
import edu.gatech.i3l.HealthPort.ports.HealthVaultPort;
import edu.gatech.i3l.HealthPort.ports.ExactDataPort;

public class ImmunizationResourceProvider implements IResourceProvider {
	private HealthPortInfo healthPortUser;
//	private ExactDataPort syntheticEHRPort;
//	private HealthVaultPort healthvaultPort;
//	private GreenwayPort greenwayPort;
//	private ExactDataPort syntheticCancerPort;

	// Constructor
	public ImmunizationResourceProvider() {
		healthPortUser = new HealthPortInfo("jdbc/HealthPort");
		
		
//		syntheticEHRPort = new ExactDataPort("jdbc/ExactDataSample",
//				ExactDataPort.SyntheticEHR);
//		healthvaultPort = new HealthVaultPort();
//		greenwayPort = new GreenwayPort();
//		syntheticCancerPort = new ExactDataPort("jdbc/ExactDataCancer",
//				ExactDataPort.SyntheticCancer);
	}

	@Override
	public Class<? extends IResource> getResourceType() {
		// TODO Auto-generated method stub
		return Immunization.class;
	}

	@Read()
	public Immunization getResourceById(@IdParam IdDt theId) {
		List<String>Ids = new ArrayList<String>();
		Ids.add(theId.getIdPart());
		
		List<IResource> resourceList = null;
		try {
			resourceList = healthPortUser.getResourceList(HealthPortInfo.IMMUNIZATION, Ids);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (resourceList == null || resourceList.isEmpty()) return null;
		return (Immunization) resourceList.get(0);		
	}

	@Search
	public IBundleProvider getAllImmunizations() {
		final InstantDt searchTime = InstantDt.withCurrentTime();
		final List<String> matchingResourceIds = healthPortUser
				.getAllResourceIds(HealthPortInfo.IMMUNIZATION);

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
					retVal = healthPortUser.getResourceList(HealthPortInfo.IMMUNIZATION,
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

	}

	@Search()
	public IBundleProvider getImmunizationsByPatient(
			@RequiredParam(name = Immunization.SP_SUBJECT) ReferenceParam theSubject) {

		final InstantDt searchTime = InstantDt.withCurrentTime();
		String patientID = theSubject.getIdPart();

		final List<String> matchingResourceIds = healthPortUser.getResourceIdsByPatient(HealthPortInfo.IMMUNIZATION, patientID);

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
					retVal = healthPortUser.getResourceList(HealthPortInfo.IMMUNIZATION,
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

	}
}