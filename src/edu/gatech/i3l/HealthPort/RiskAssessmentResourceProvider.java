package edu.gatech.i3l.HealthPort;

import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.dev.resource.RiskAssessment;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;

public class RiskAssessmentResourceProvider implements IResourceProvider {
	
	private SyntheticEHRPort syntheticEHRPort;
	private HealthVaultPort healthvaultPort;
	
	public RiskAssessmentResourceProvider () {
		syntheticEHRPort = new SyntheticEHRPort();
		healthvaultPort = new HealthVaultPort();
	}
	public Class<RiskAssessment> getResourceType() {
		return RiskAssessment.class;
	}
	
	
}