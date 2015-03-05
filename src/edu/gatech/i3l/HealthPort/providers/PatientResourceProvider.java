package edu.gatech.i3l.HealthPort.providers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
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
import edu.gatech.i3l.HealthPort.HealthPortInfo;
import edu.gatech.i3l.HealthPort.ports.ExactDataPort;
import edu.gatech.i3l.HealthPort.ports.GreenwayPort;
import edu.gatech.i3l.HealthPort.ports.HealthVaultPort;

/**
 * All resource providers must implement IResourceProvider
 */
public class PatientResourceProvider implements IResourceProvider {
	public static final String SQL_STATEMENT = "SELECT U1.ID, U1.ORGANIZATIONID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID)";
 
	private HealthPortInfo healthPortUser;
	private String GWID;
	private String HVID;
	
	public PatientResourceProvider () {
		healthPortUser = new HealthPortInfo ("jdbc/HealthPort");
		GWID = healthPortUser.getOrgID(GreenwayPort.GREENWAY);
		HVID = healthPortUser.getOrgID(HealthVaultPort.HEALTHVAULT);
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
    	
    	String Ids[] = theId.getIdPart().split("\\.", 2);
    	String patientID;
    	
    	if (Ids.length != 2) {
    		return patient;
    	}
    	if (Ids[0].equals(GWID) ||
    			Ids[0].equals(HVID)) {
    		healthPortUser.setInformation(Ids[1]);
    		patientID = healthPortUser.orgID+"."+healthPortUser.userId;
    	} else {
    		healthPortUser.setInformationPersonID(Ids[1], Ids[0]);
    		patientID = healthPortUser.orgID+"."+healthPortUser.personId;
    	}
    	
    	if (healthPortUser.orgID == null) return null;

		patient.addIdentifier();
		patient.getIdentifier().get(0).setSystem(new UriDt("urn:healthport:mrns"));
		patient.setId(patientID);
		patient.getIdentifier().get(0).setValue(patientID);

		String[] userName  = healthPortUser.name.split(" ");
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
		//patient.getText().setDiv(output);
    	
    	return patient;
    }
     
    
    @Search
    public List<Patient> getAllPatients() {
    	//System.out.println("HERE");
    	Connection connection = null;
		Statement statement = null;
				
		ArrayList<Patient> retVal = new ArrayList<Patient>();
    	try{
    		connection = healthPortUser.getConnection();
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			while (resultSet.next()) {
				healthPortUser.setRSInformation(resultSet);
				String[] userName  = healthPortUser.name.split(" ");
				
				Patient patient = new Patient();
				patient.addIdentifier();
				patient.getIdentifier().get(0).setSystem(new UriDt("urn:healthport:mrns"));
				if (healthPortUser.orgID.equals(GWID) ||
						healthPortUser.orgID.equalsIgnoreCase(HVID)) {
					patient.setId(healthPortUser.orgID+"."+healthPortUser.userId);
					patient.getIdentifier().get(0).setValue(healthPortUser.orgID+"."+healthPortUser.userId);
				} else {
					patient.setId(healthPortUser.orgID+"."+healthPortUser.personId);
					patient.getIdentifier().get(0).setValue(healthPortUser.orgID+"."+healthPortUser.personId);
				}
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
				patient.getText().setStatus(NarrativeStatusEnum.GENERATED);
				String textBody = "<table>"
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