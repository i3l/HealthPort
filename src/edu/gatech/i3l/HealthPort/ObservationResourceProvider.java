package edu.gatech.i3l.HealthPort;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.resource.Patient;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

public class ObservationResourceProvider implements IResourceProvider {
	public static final String SQL_STATEMENT = "SELECT ID FROM USER";

    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    public Class<Observation> getResourceType() {
        return Observation.class;
    }
    
    @Read()
    public Observation getResourceById(@IdParam IdDt theId){
    	Observation obs = new Observation();
    	String resourceId = theId.getIdPart();
    	String[] Ids  = theId.getIdPart().split("\\-",3);
    	
    	HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(Integer.parseInt(Ids[0]));
    	String location = HealthPortUser.dataSource;
		
    	if(location.equals(HealthPortUserInfo.GREENWAY)){
			System.out.println("Greenway");
	     	
		} else if (location.equals(HealthPortUserInfo.SyntheticEHR)) {
			//obs = new SyntheticEHRPort().getObservations(HealthPortUser);
			
		} else if(location.equals(HealthPortUserInfo.HEALTHVAULT)){
			obs = new HealthVaultPort().getObservation(resourceId);
		}
    
    	return obs; 	
    }
     
    @Search
    public List<Observation> getAllObservations() {
    	Connection connection = null;
		Statement statement = null;
		Context context = null;
		DataSource datasource = null;
		String location=null;
		String ccd=null;
		
		ArrayList<Observation> finalRetVal = new ArrayList<Observation>();
		ArrayList<Observation> retVal = new ArrayList<Observation>();
    	try{
			context = new InitialContext();
			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/HealthPort");
			connection = datasource.getConnection();
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			while (resultSet.next()) {
				//String Name = resultSet.getString("NAME");
				int userId = resultSet.getInt("ID");
				
				HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(userId);
		    	String rId = HealthPortUser.recordId;
		    	String pId = HealthPortUser.personId;
		    	location = HealthPortUser.dataSource;
		    
		    	if(location.equals(HealthPortUserInfo.GREENWAY)){
					ccd = GreenwayPort.getCCD(pId);
					System.out.println(ccd);
			     	
				} else if (location.equals(HealthPortUserInfo.SyntheticEHR)) {
					retVal = new SyntheticEHRPort().getObservations(HealthPortUser);
					finalRetVal.addAll(retVal);
					
				} else if(location.equals(HealthPortUserInfo.HEALTHVAULT)){
					retVal = new HealthVaultPort().getObservations(HealthPortUser);
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
    public List<Observation> getObservationsbyPatient(
    		@RequiredParam(name=Observation.SP_SUBJECT) ReferenceParam theSubject){
    	
		String location=null;
		String ccd=null;	
		
  
    	ArrayList<Observation> retVal = new ArrayList<Observation>();
    	String PatientID;
 
    	if (theSubject.hasResourceType()) {
    		String resourceType = theSubject.getResourceType();
    		if ("Patient".equals(resourceType) == false) {
    			throw new InvalidRequestException("Invalid resource type for parameter 'subject': " + resourceType);
    		} else {
    			PatientID = theSubject.getIdPart();
    		}
    	} else {
    		throw new InvalidRequestException("Need resource type for parameter 'subject'");
    	}
    	
    	int patientNum = Integer.parseInt(PatientID);
		
    	HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(patientNum);
    	String rId = HealthPortUser.recordId;
    	String pId = HealthPortUser.personId;
    	location = HealthPortUser.dataSource;
    	
//		try{
//			context = new InitialContext();
//			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/HealthPort");
//			connection = datasource.getConnection();
//			statement = connection.createStatement();
//			String SQL_STATEMENT = "SELECT U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID) WHERE U1.ID="+patientNum;
//			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
//			if (resultSet.next()) {
//				name = resultSet.getString("NAME");
//				location = resultSet.getString("TAG");
//				rId = resultSet.getString("RECORDID");
//				pId = resultSet.getString("PERSONID");
//				System.out.println(patientNum);
//				System.out.println(name+":"+location);
//			} else {
//				return retVal;
//			}
//			
//			connection.close();
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		//Access Greenway and get CCD of patient
		if(location.equals(HealthPortUserInfo.GREENWAY)){
			ccd = GreenwayPort.getCCD(pId);
			System.out.println(ccd);
	     	
		} else if (location.equals(HealthPortUserInfo.SyntheticEHR)) {
			retVal = new SyntheticEHRPort().getObservations(HealthPortUser);
			
		} else if(location.equals(HealthPortUserInfo.HEALTHVAULT)){
			retVal = new HealthVaultPort().getObservations(HealthPortUser);
		}

       return retVal;

    }
    
 
 
}