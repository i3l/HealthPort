package edu.gatech.i3l.HealthPort.providers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Immunization;
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
import edu.gatech.i3l.HealthPort.HealthPortInfo;

/**
 * Retrieve Immunization data as HAPI-FHIR resource objects.
 * @author Eric Wallace <ewall@gatech.edu>
 */
public class ImmunizationResourceProvider implements IResourceProvider {
	private HealthPortInfo healthPortUser;
	
	public ImmunizationResourceProvider() {
		healthPortUser = new HealthPortInfo("jdbc/HealthPort");
	}
	
	/* (non-Javadoc)
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */
	@Override
	public Class<? extends IResource> getResourceType() {
		return Immunization.class;
	}

	/**
	 * Given a single resource, retrieve that Immunization resource.
	 * @param theId
	 * @return an Immunization object
	 */
	@Read()
	public Immunization getResourceById(@IdParam IdDt theId) {
		List<String>Ids = new ArrayList<String>();
		Ids.add(theId.getIdPart());
		
		List<IResource> resourceList = null;
		try {
			resourceList = healthPortUser.getResourceList(HealthPortInfo.IMMUNIZATION, Ids);
		} catch (SQLException e) {
			e.printStackTrace(); // TODO Auto-generated catch block
		}
	
		if (resourceList == null || resourceList.isEmpty()) return null;
		return (Immunization) resourceList.get(0);		
	}
	
	/** 
	 * Retrieve all available Immunization resources.
	 * @return a Bundle of Immunization objects
	 */
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
				List<String> idsToReturn = matchingResourceIds.subList(theFromIndex, end);
				List<IResource> retVal = null;
				try {
					retVal = healthPortUser.getResourceList(HealthPortInfo.IMMUNIZATION, idsToReturn);
				} catch (SQLException e) {
					e.printStackTrace(); // TODO Auto-generated catch block
				}
				
				return retVal;
			}

			@Override
			public InstantDt getPublished() {
				return searchTime;
			}
		};
	}
	
	/** 
	 * Retrieve all available Immunizations associated with a patient.
	 * @param theSubject a Patient object
	 * @return a Bundle of Immunization objects
	 */
	@Search()
	public IBundleProvider getImmunizationsByPatient(
			@RequiredParam(name = Immunization.SP_SUBJECT) ReferenceParam theSubject) {

		final InstantDt searchTime = InstantDt.withCurrentTime();
		String patientID = theSubject.getIdPart();

		final List<String> matchingResourceIds = healthPortUser
				.getResourceIdsByPatient(HealthPortInfo.IMMUNIZATION, patientID);

		return new IBundleProvider() {

			@Override
			public int size() {
				return matchingResourceIds.size();
			}

			@Override
			public List<IResource> getResources(int theFromIndex, int theToIndex) {
				int end = Math.min(theToIndex, matchingResourceIds.size());
				// System.out.println("From:"+theFromIndex+" To:"+theToIndex+" Total:"+matchingResourceIds.size());
				List<String> idsToReturn = matchingResourceIds.subList(theFromIndex, end);
				
				List<IResource> retVal = null;
				try {
					retVal = healthPortUser.getResourceList(HealthPortInfo.IMMUNIZATION,idsToReturn);
				} catch (SQLException e) {
					e.printStackTrace(); // TODO Auto-generated catch block
				}
				
				return retVal;
			}

			@Override
			public InstantDt getPublished() {
				return searchTime;
			}
		};

	}
	
	/**
	 * @param theId search parameter
	 * @return a Bundle of Immunization objects
	 */
	@Search()
	public IBundleProvider searchByVaccineType(
			@RequiredParam(name = Immunization.SP_VACCINE_TYPE) TokenParam theId) {
		// TODO implement searchByVaccineType()
		throw new UnsupportedOperationException("Not implemented yet");
	}
	
}
