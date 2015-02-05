package edu.gatech.i3l.HealthPort;


import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;


public class ConditionResourceProvider implements IResourceProvider {

	@Override
	public Class<? extends IResource> getResourceType() {
		// TODO Auto-generated method stub
		return Condition.class;
	}
	
	@Read()
	 public Condition getResourceById(@IdParam IdDt theId){
    	Condition cond = new Condition();
    	String resourceId = theId.getIdPart();
    	String[] Ids  = theId.getIdPart().split("\\-",3);
    	
    	HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(Integer.parseInt(Ids[0]));
    	String location = HealthPortUser.dataSource;
		
    	if(location.equals(HealthPortUserInfo.GREENWAY)){
			System.out.println("Greenway");
	     	
		} else if (location.equals(HealthPortUserInfo.SyntheticEHR)) {
			cond = new SyntheticEHRPort().getCondition(resourceId);
			
		} else if(location.equals(HealthPortUserInfo.HEALTHVAULT)){
			cond = new HealthVaultPort().getCondition(resourceId);
		}
    	
    	
    	return cond; 	
    }

	@Search()
	public List<Condition> getConditionsByPatient(
			@RequiredParam(name=Condition.SP_SUBJECT) ReferenceParam theSubject) {

		String location=null;
		String ccd=null;
    	int patientNum = Integer.parseInt(theSubject.getIdPart());
		
		ArrayList<Condition> retVal = new ArrayList<Condition>();  
		
		HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(patientNum);
    	String rId = HealthPortUser.recordId;
    	String pId = HealthPortUser.personId;
    	location = HealthPortUser.dataSource;
    
    	if(location.equals(HealthPortUserInfo.GREENWAY)){
    			ccd = GreenwayPort.getCCD(pId);
    			System.out.println(ccd);
    	}
    	else if(location.equals(HealthPortUserInfo.SyntheticEHR)){ 			
    		retVal = new SyntheticEHRPort().getConditions(HealthPortUser);
 
    	}else if(location.equals(HealthPortUserInfo.HEALTHVAULT)){
    		retVal = new HealthVaultPort().getConditions(HealthPortUser);
		}
		
		return retVal;
	}
<<<<<<< HEAD
=======
	
	@Search()
	public List<Condition> searchByIdentifier(@RequiredParam(name=Condition.SP_CODE) TokenParam theId) {
	   String identifierSystem = theId.getSystem();
	   String identifier = theId.getValue();
	   System.out.println(identifierSystem);
	   System.out.println(identifier);
	   ArrayList<Condition> retVal = new ArrayList<Condition>(); 
	   retVal = new SyntheticEHRPort().getConditionsByType(identifier);
	   return retVal;
	}

>>>>>>> FHIR-integrate

}