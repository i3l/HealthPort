/**
 * 
 */
package edu.gatech.i3l.HealthPort.providers;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dev.resource.RiskAssessment;
import ca.uhn.fhir.rest.server.IResourceProvider;

/**
 * @author MC142
 *
 */
public class RiskAssessmentResourceProvider implements IResourceProvider {

	/* (non-Javadoc)
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */
	@Override
	public Class<RiskAssessment> getResourceType() {
		// TODO Auto-generated method stub
		return RiskAssessment.class;
	}

}
