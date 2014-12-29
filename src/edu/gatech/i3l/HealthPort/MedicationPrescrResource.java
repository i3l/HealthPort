package edu.gatech.i3l.HealthPort;

import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;


public class MedicationPrescrResource implements IResourceProvider {

	/* (non-Javadoc)
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */
	@Override
	public Class<? extends IResource> getResourceType() {
		// TODO Auto-generated method stub
		return MedicationPrescription.class;
	}
	
	@Search()
	public List<MedicationPrescription> getMedicationPrescriptionsByPatient(
			@RequiredParam(name=Condition.SP_SUBJECT) ReferenceParam theSubject) {
		
		ArrayList<MedicationPrescription> retVal = new ArrayList<MedicationPrescription>(); 
		String location=null;
		String ccd=null;
    	int patientNum = Integer.parseInt(theSubject.getIdPart());
    	HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(patientNum);
    	String rId = HealthPortUser.recordId;
    	String pId = HealthPortUser.personId;
    	location = HealthPortUser.dataSource;
    
    	if(location.equals(HealthPortUserInfo.GREENWAY)){
    			ccd = GreenwayPort.getCCD(pId);
    			System.out.println(ccd);
    	}
    	else if(location.equals(HealthPortUserInfo.SyntheticEHR)){ 			
    		retVal = new SyntheticEHRPort().getMedicationPrescriptions(HealthPortUser);
 
    	}else if(location.equals(HealthPortUserInfo.HEALTHVAULT)){
    		retVal = new HealthVaultPort().getMedicationPrescriptions(HealthPortUser);
		}
		
		return retVal;
	}
}