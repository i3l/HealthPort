package edu.gatech.i3l.HealthPort.providers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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
	public static final String SQL_STATEMENT = "SELECT person_id FROM person";
 
	private Context context;
	private DataSource datasource;
	
	public PatientResourceProvider () {
		try {
			context = new InitialContext();
			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/omop_v4_mimic2");
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
      
    
    @Search
    public List<Patient> getAllPatients() {
    	//System.out.println("HERE");
    	Connection connection = null;
		Statement statement = null;
				
		ArrayList<Patient> retVal = new ArrayList<Patient>();
		int count = 1;
    	try{
    		connection = datasource.getConnection();
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			while (resultSet.next()) {
				int userId = resultSet.getInt("person_id");
		
				Patient patient = new Patient();
				patient.setId(String.valueOf(userId));
				patient.addIdentifier();
				patient.getIdentifier().get(0).setSystem(new UriDt("urn:hapitest:mrns"));
				patient.getIdentifier().get(0).setValue(String.valueOf(userId));
	
		        retVal.add(patient);
			}
			connection.close();
			
    	} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {
				connection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    
       return retVal;

    }
 
}