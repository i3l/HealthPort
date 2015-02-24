/**
 * 
 */
package edu.gatech.i3l.HealthPort.providers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dev.resource.Condition;
import ca.uhn.fhir.model.dev.resource.Observation;
import ca.uhn.fhir.model.dev.resource.OperationOutcome;
import ca.uhn.fhir.model.dev.resource.Patient;
import ca.uhn.fhir.model.dev.resource.RiskAssessment;
import ca.uhn.fhir.model.dev.valueset.IssueSeverityEnum;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

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
	@Read()
	public RiskAssessment getResourceById(@IdParam IdDt theId) {
		RiskAssessment risk = null;
		
		return risk;
		
	}

	@Search()
	public List<RiskAssessment> getRiskAssessmentbySubject(
			@RequiredParam(name = Condition.SP_SUBJECT) ReferenceParam theSubject) {
		ArrayList<RiskAssessment> retV = null;
		return retV;
		
	}
	
	
	@Create()
	public MethodOutcome createRiskAssessment(@ResourceParam RiskAssessment theRisk){
		validateResource(theRisk);
		MethodOutcome retVal = new MethodOutcome();
		System.out.println("In risk assessment");
		
		int number1 = 4;
		Process p = null;
		int ret = 0;
		//String pbCommand[] = { "python", "/Users/ameliahenderson/Desktop/test.py",""+number1};
		//ProcessBuilder pb = new ProcessBuilder(pbCommand);
		//Process pb = Runtime.getRuntime().exec(pbCommand);
		//ProcessBuilder pb = new ProcessBuilder("python","test.py",""+number1);
		//pb.directory(new File("\\Users\\ameliahenderson\\Desktop"));
		try {
			String pbCommand[] = { "python", "/Users/ameliahenderson/Desktop/test.py"," "+number1};
			//ProcessBuilder pb = new ProcessBuilder(pbCommand);
			Process pb = Runtime.getRuntime().exec(pbCommand);
			 BufferedReader stdInput = new BufferedReader(new InputStreamReader(pb.getInputStream()));
			 //BufferedReader stdError = new BufferedReader(new InputStreamReader(pb.getErrorStream()));
			// read the output

	            String s;
				while ((s = stdInput.readLine()) != null) {

	                System.out.println(s);

	            }
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		return retVal;
	}
	
	private void validateResource(RiskAssessment theRisk) {
		/*
		 * Our server will have a rule that patients must have a family name or we will reject them
		 */
		if (theRisk.getSubject().isEmpty()) {
			OperationOutcome outcome = new OperationOutcome();
			outcome.addIssue().setSeverity(IssueSeverityEnum.FATAL).setDetails("No subject provided,RiskAssessment resources must have at least one subject.");
			throw new UnprocessableEntityException(outcome);
		}
	}
}
