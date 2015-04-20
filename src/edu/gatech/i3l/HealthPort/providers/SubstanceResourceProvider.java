package edu.gatech.i3l.HealthPort.providers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Substance;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import edu.gatech.i3l.HealthPort.HealthPortInfo;

/**
 * Retrieve Substance data as HAPI-FHIR resource objects.
 * @author Eric Wallace <ewall@gatech.edu>
 */
public class SubstanceResourceProvider implements IResourceProvider {
	private HealthPortInfo healthPortUser;
	
	public SubstanceResourceProvider() {
		healthPortUser = new HealthPortInfo("jdbc/HealthPort");
	}
	
	/* (non-Javadoc)
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */
	@Override
	public Class<? extends IResource> getResourceType() {
		return Substance.class;
	}

	/**
	 * Given a single resource, retrieve that Substance resource.
	 * @param theId
	 * @return an Substance object
	 */
	@Read()
	public Substance getResourceById(@IdParam IdDt theId) {
		List<String>Ids = new ArrayList<String>();
		Ids.add(theId.getIdPart());

		List<IResource> resourceList = null;
		try {
			resourceList = healthPortUser.getResourceList(HealthPortInfo.SUBSTANCE, Ids);
		} catch (SQLException e) {
			e.printStackTrace(); // TODO Auto-generated catch block
		}

		if (resourceList == null || resourceList.isEmpty()) return null;
		return (Substance) resourceList.get(0);		
	}
	
	/** 
	 * Retrieve all available Substance resources.
	 * @return a Bundle of Substance objects
	 */
	@Search
	public IBundleProvider getAllSubstances() {
		final InstantDt searchTime = InstantDt.withCurrentTime();
		final List<String> matchingResourceIds = healthPortUser
				.getAllResourceIds(HealthPortInfo.SUBSTANCE);

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
					retVal = healthPortUser.getResourceList(HealthPortInfo.SUBSTANCE, idsToReturn);
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
