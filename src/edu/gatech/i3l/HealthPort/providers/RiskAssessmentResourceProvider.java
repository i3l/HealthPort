/**
 * 
 */
package edu.gatech.i3l.HealthPort.providers;

import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dev.resource.Condition;
import ca.uhn.fhir.model.dev.resource.RiskAssessment;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;

/**
 * @author MC142
 *
 */
public class RiskAssessmentResourceProvider implements IResourceProvider {

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */
	@Override
	public Class<RiskAssessment> getResourceType() {
		// TODO Auto-generated method stub
		return RiskAssessment.class;
	}

	@Search()
	public List<RiskAssessment> getRiskAssessmentbySubject(
			@RequiredParam(name = Condition.SP_SUBJECT) ReferenceParam theSubject) {
		ArrayList<RiskAssessment> retV = null;
		return retV;
		
	}
	
	@Create()
	public MethodOutcome createRiskAssessment(@ResourceParam RiskAssessment theRisk){
		MethodOutcome retVal = new MethodOutcome();
		System.out.println("In risk assessment");
		return retVal;
	}
}
