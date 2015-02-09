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
    	FhirContext ctx = new FhirContext();
    	int id = Integer.parseInt(theId.getIdPart());
    	HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(id);
    	
		patient.setId(HealthPortUser.userId);
		patient.addIdentifier();
		patient.getIdentifier().get(0).setSystem(new UriDt("urn:hapitest:mrns"));
		patient.getIdentifier().get(0).setValue(HealthPortUser.userId);
		String[] userName  = HealthPortUser.name.split(" ");
        if (userName.length == 2){
            patient.addName().addFamily(userName[1]);
            patient.getName().get(0).addGiven(userName[0]);
        }
        else{
            patient.addName().addFamily(userName[2]);
            patient.getName().get(0).addGiven(userName[0]+ " "+ userName[1]);
        }
		ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		// Encode the output, including the narrative
		String output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(patient);
		patient.getText().setDiv(output);
    	
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
		        NarrativeStatusEnum narrative = null;
				patient.getText().setStatus(narrative.GENERATED);
				StringBuffer buffer_narrative = new StringBuffer();
		        buffer_narrative.append("<div>\n");
				buffer_narrative.append("<div class=\"hapiHeaderText\">" + fullName+ "</div>\n");
				buffer_narrative.append("<table class=\"hapiPropertyTable\">\n");
				buffer_narrative.append("	<tbody>\n");
				buffer_narrative.append("		<tr>\n");
				buffer_narrative.append("			<td>Name</td>\n");
				buffer_narrative.append("			<td>"+ fullName+ "</td>\n");
				buffer_narrative.append("		</tr>\n");
				buffer_narrative.append("	</tbody>\n");
				buffer_narrative.append("</table>\n");
				buffer_narrative.append("</div>\n");
				String output = buffer_narrative.toString();
			    patient.getText().setDiv(output);
		        retVal.add(patient);
			}
			connection.close();
			
		} catch (Exception e){
			e.printStackTrace();
		}
    
       return retVal;

    }
 
}