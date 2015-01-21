package edu.gatech.i3l.HealthPort;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;


public class MedicationPrescrResource implements IResourceProvider {
	public static final String SQL_STATEMENT = "SELECT ID FROM USER";

	/* (non-Javadoc)
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */
	@Override
	public Class<? extends IResource> getResourceType() {
		// TODO Auto-generated method stub
		return MedicationPrescription.class;
	}
	
	@Read()
    public MedicationPrescription getResourceById(@IdParam IdDt theId){
    	MedicationPrescription med = new MedicationPrescription();
    	String resourceId = theId.getIdPart();
    	String[] Ids  = theId.getIdPart().split("\\-",3);
    	
    	HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(Integer.parseInt(Ids[0]));
    	String location = HealthPortUser.dataSource;
		
    	if(location.equals(HealthPortUserInfo.GREENWAY)){
			System.out.println("Greenway");
	     	
		} else if (location.equals(HealthPortUserInfo.SyntheticEHR)) {
			//med = new SyntheticEHRPort().getObservations(HealthPortUser);
			
		} else if(location.equals(HealthPortUserInfo.HEALTHVAULT)){
			med = new HealthVaultPort().getMedicationPrescription(resourceId);
		}
    
    	return med; 	
    }
	
	 @Search
	    public List<MedicationPrescription> getAllMedicationPrescription() {
	    	Connection connection = null;
			Statement statement = null;
			Context context = null;
			DataSource datasource = null;
			String location=null;
			String ccd=null;
			
			ArrayList<MedicationPrescription> finalRetVal = new ArrayList<MedicationPrescription>();
			ArrayList<MedicationPrescription> retVal = new ArrayList<MedicationPrescription>();
	    	try{
				context = new InitialContext();
				datasource = (DataSource) context.lookup("java:/comp/env/jdbc/HealthPort");
				connection = datasource.getConnection();
				statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
				while (resultSet.next()) {
					int userId = resultSet.getInt("ID");
					
					HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(userId);
			    	String rId = HealthPortUser.recordId;
			    	String pId = HealthPortUser.personId;
			    	location = HealthPortUser.dataSource;
			    
			    	if(location.equals(HealthPortUserInfo.GREENWAY)){
		    			ccd = GreenwayPort.getCCD(pId);
		    			System.out.println(ccd);
			    	}
			    	else if(location.equals(HealthPortUserInfo.SyntheticEHR)){ 			
			    		retVal = new SyntheticEHRPort().getMedicationPrescriptions(HealthPortUser);
			    		finalRetVal.addAll(retVal);
		 
			    	}else if(location.equals(HealthPortUserInfo.HEALTHVAULT)){
			    		retVal = new HealthVaultPort().getMedicationPrescriptions(HealthPortUser);
			    		finalRetVal.addAll(retVal);
			    	}
			    	
			    	retVal.clear();
		
				}
				connection.close();
				
			} catch (Exception e){
				e.printStackTrace();
			}
		
			return finalRetVal;
	    	
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