package edu.gatech.i3l.HealthPort.providers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dev.resource.Patient;
//import ca.uhn.fhir.model.dev.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import edu.gatech.i3l.HealthPort.HealthPortUserInfo;

/**
 * All resource providers must implement IResourceProvider
 */
public class PatientResourceProvider implements IResourceProvider {
	public static final String SQL_STATEMENT = "SELECT U1.ID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID)";
 
	private HealthPortUserInfo healthPortUser;
	
	public PatientResourceProvider () {
		healthPortUser = new HealthPortUserInfo ("jdbc/HealthPort");
	}
	
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }
    
    @Read()
    public Patient getResourceById(@IdParam IdDt theId){
    	Patient patient = new Patient();
    	FhirContext ctx = new FhirContext();
//    	int id = Integer.parseInt(theId.getIdPart());
//    	HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(id);
    	healthPortUser.setInformation(theId.getIdPart());
    	if (healthPortUser.source == null) return patient;
    	
		patient.setId(healthPortUser.userId);
		patient.addIdentifier();
		patient.getIdentifier().get(0).setSystem(new UriDt("urn:hapitest:mrns"));
		patient.getIdentifier().get(0).setValue(healthPortUser.userId);
		String[] userName  = healthPortUser.name.split(" ");
		String fullName = null;
        if (userName.length == 2){
            patient.addName().addFamily(userName[1]);
            patient.getName().get(0).addGiven(userName[0]);
            fullName = userName[0] + " "+userName[1];
        }
        else{
            patient.addName().addFamily(userName[2]);
            patient.getName().get(0).addGiven(userName[0]+ " "+ userName[1]);
            fullName = userName[0] + " "+userName[1]+ " "+userName[2];
        }
        String textBody = "<table class=\"hapiPropertyTable\">"
				+ "<tr><td>Name</td><td>"+ fullName
				+ "</td></tr></table>";
	    patient.getText().setDiv(textBody);
    	
    	return patient;
    }
     
    
    @Search
    public List<Patient> getAllPatients() {
    	//System.out.println("HERE");
    	Connection connection = null;
		Statement statement = null;
		Context context = null;
		DataSource datasource = null;
		FhirContext ctx = new FhirContext();
				
		ArrayList<Patient> retVal = new ArrayList<Patient>();
    	try{
//			context = new InitialContext();
//			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/HealthPort");
//			connection = datasource.getConnection();
    		connection = healthPortUser.getConnection();
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			while (resultSet.next()) {
//				String Name = resultSet.getString("NAME");
//				int userId = resultSet.getInt("ID");
				
				healthPortUser.setRSInformation(resultSet);
				String[] userName  = healthPortUser.name.split(" ");
				
				Patient patient = new Patient();
//				patient.setId(String.valueOf(userId));
				patient.setId(healthPortUser.userId);
				patient.addIdentifier();
				patient.getIdentifier().get(0).setSystem(new UriDt("urn:hapitest:mrns"));
//				patient.getIdentifier().get(0).setValue(String.valueOf(userId));
				patient.getIdentifier().get(0).setValue(healthPortUser.userId);
				String fullName = null;
		        if (userName.length == 2){
                    patient.addName().addFamily(userName[1]);
                    patient.getName().get(0).addGiven(userName[0]);
                    fullName = userName[0] + " "+userName[1];
		        }
		        else{
                    patient.addName().addFamily(userName[2]);
                    patient.getName().get(0).addGiven(userName[0]+ " "+ userName[1]);
                    fullName = userName[0] + " "+userName[1]+ " "+userName[2];
		        }
		        
				//ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
				// Encode the output, including the narrative
				//String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(patient);
				//patient.getText().setStatus(ca.uhn.fhir.model.dstu.valueset.NarrativeStatusEnum.GENERATED);
				String textBody = "<table class=\"hapiPropertyTable\">"
						+ "<tr><td>Name</td><td>"+ fullName
						+ "</td></tr></table>";
			    patient.getText().setDiv(textBody);
		        retVal.add(patient);
			}
			connection.close();
			
		} catch (Exception e){
			e.printStackTrace();
		}
    
       return retVal;

    }
 
}