package edu.gatech.i3l.HealthPort;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.dstu.resource.Patient;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;

/**
 * All resource providers must implement IResourceProvider
 */
public class PatientResourceProvider implements IResourceProvider {
	public static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	public static final String JDBC_URL = "jdbc:derby:" + Config.DBPath+"/MyDB;user=adh;password=123";
	public static final String SQL_STATEMENT = "select * from cdcAppDB.Users";
 
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
			Class.forName(DRIVER);
			connection = DriverManager.getConnection(JDBC_URL);
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			while (resultSet.next()) {
				String Name = resultSet.getString("name");
				int userId = resultSet.getInt("id");
				String[] userName  = Name.split(" ");
				
				Patient patient = new Patient();
				patient.setId(String.valueOf(userId));
				patient.addIdentifier();
				patient.getIdentifier().get(0).setSystem(new UriDt("urn:hapitest:mrns"));
				patient.getIdentifier().get(0).setValue(String.valueOf(count));
				patient.addName().addFamily(userName[1]);
				patient.getName().get(0).addGiven(userName[0]);

		       retVal.add(patient);
		       count = count+1;
				//System.out.println(Name + "\n");
			}
			connection.close();
			
		} catch (ClassNotFoundException e){
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{
				if(statement != null) statement.close();
				if(connection != null) connection.close();
			}catch (SQLException e){e.printStackTrace();}
		}
    	
       return retVal;

    }
 
}