package edu.gatech.i3l.HealthPort;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu.resource.Patient;
import ca.uhn.fhir.model.dstu.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;

/**
 * All resource providers must implement IResourceProvider
 */
public class PatientResourceProvider implements IResourceProvider {
	public static final String SQL_STATEMENT = "SELECT * FROM USER";
 
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
    	int id = Integer.parseInt(theId.getIdPart());
    	HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(id);
    	
		patient.setId(HealthPortUser.userId);
		patient.addIdentifier();
		patient.getIdentifier().get(0).setSystem(new UriDt("urn:hapitest:mrns"));
		patient.getIdentifier().get(0).setValue(HealthPortUser.userId);
		String[] userName  = HealthPortUser.name.split(" ");
		patient.addName().addFamily(userName[1]);
		patient.getName().get(0).addGiven(userName[0]);
    	
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
			context = new InitialContext();
			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/HealthPort");
			connection = datasource.getConnection();
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			while (resultSet.next()) {
				String Name = resultSet.getString("NAME");
				int userId = resultSet.getInt("ID");
				String[] userName  = Name.split(" ");
				
				Patient patient = new Patient();
				patient.setId(String.valueOf(userId));
				patient.addIdentifier();
				patient.getIdentifier().get(0).setSystem(new UriDt("urn:hapitest:mrns"));
				patient.getIdentifier().get(0).setValue(String.valueOf(userId));
				patient.addName().addFamily(userName[1]);
				patient.getName().get(0).addGiven(userName[0]);
				//patient.getText().setStatus(NarrativeStatusEnum.GENERATED);

		       retVal.add(patient);
		       String output;
		       //ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		       //output = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
		       //output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(patient);
		       //patient.getText().setDiv(output);
			}
			connection.close();
			
		} catch (Exception e){
			e.printStackTrace();
		}
    
       return retVal;

    }
 
}