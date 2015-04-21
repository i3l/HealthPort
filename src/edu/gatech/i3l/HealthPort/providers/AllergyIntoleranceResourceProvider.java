package edu.gatech.i3l.HealthPort.providers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.AllergyIntolerance;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import edu.gatech.i3l.HealthPort.HealthPortInfo;

/**
 * Retrieve AllergyIntolerance data as HAPI-FHIR resource objects.
 * @author Eric Wallace <ewall@gatech.edu>
 */
public class AllergyIntoleranceResourceProvider implements IResourceProvider {
	private HealthPortInfo healthPortUser;
	
	public AllergyIntoleranceResourceProvider() {
		healthPortUser = new HealthPortInfo("jdbc/HealthPort");
	}
	
	/* (non-Javadoc)
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */
	@Override
	public Class<? extends IResource> getResourceType() {
		return AllergyIntolerance.class;
	}

	/**
	 * Given a single resource ID, retrieve that AllergyIntolerance resource.
	 * @param theId
	 * @return an AllergyIntolerance object
	 */
	@Read()
	public AllergyIntolerance getResourceById(@IdParam IdDt theId) {
		List<String>Ids = new ArrayList<String>();
		Ids.add(theId.getIdPart());

		List<IResource> resourceList = null;
		try {
			resourceList = healthPortUser.getResourceList(HealthPortInfo.ALLERGYINTOLERANCE, Ids);
		} catch (SQLException e) {
			e.printStackTrace(); // TODO Auto-generated catch block
		}

		if (resourceList == null || resourceList.isEmpty()) return null;
		return (AllergyIntolerance) resourceList.get(0);		
	}
	
	/** 
	 * Retrieve all available AllergyIntolerance resources.
	 * @return a Bundle of AllergyIntolerance objects
	 */
	@Search
	public IBundleProvider getAllAllergyIntolerances() {
		final InstantDt searchTime = InstantDt.withCurrentTime();
		final List<String> matchingResourceIds = healthPortUser
				.getAllResourceIds(HealthPortInfo.ALLERGYINTOLERANCE);

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
					retVal = healthPortUser.getResourceList(HealthPortInfo.ALLERGYINTOLERANCE, idsToReturn);
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
	 * Retrieve all available AllergyIntolerances associated with a patient.
	 * @param theSubject a Patient object
	 * @return a Bundle of AllergyIntolerance objects
	 */
	@Search()
	public IBundleProvider getAllergyIntolerancesByPatient(
			@RequiredParam(name = AllergyIntolerance.SP_SUBJECT) ReferenceParam theSubject) {

		final InstantDt searchTime = InstantDt.withCurrentTime();
		String patientID = theSubject.getIdPart();

		final List<String> matchingResourceIds = healthPortUser
				.getResourceIdsByPatient(HealthPortInfo.ALLERGYINTOLERANCE, patientID);

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
					retVal = healthPortUser.getResourceList(HealthPortInfo.ALLERGYINTOLERANCE,idsToReturn);
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
	 * Retrieve all available AllergyIntolerances associated with a Substance.
	 * @param theSubstance a Substance object
	 * @return a Bundle of AllergyIntolerance objects
	 */
	@Search()
	public IBundleProvider getAllergyIntolerancesBySubstance(
			@RequiredParam(name = AllergyIntolerance.SP_SUBSTANCE) ReferenceParam theSubstance) {

		final InstantDt searchTime = InstantDt.withCurrentTime();
		String substanceID = theSubstance.getIdPart();

		final List<String> matchingResourceIds = healthPortUser
				.getResourceIdsBySubstance(HealthPortInfo.ALLERGYINTOLERANCE, substanceID);

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
					retVal = healthPortUser.getResourceList(HealthPortInfo.ALLERGYINTOLERANCE,idsToReturn);
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
	
}
