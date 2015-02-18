/**
 * 
 */
package edu.gatech.i3l.HealthPort.ports;

//import java.io.PrintWriter;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Neha
 *
 */
public class GreenwayPort {

	/**
	 * 
	 */
	public GreenwayPort() {
		// TODO Auto-generated constructor stub
	}
	
	static public String getCCD (String PatientID) {
		
		Client client = ClientBuilder.newClient();

		//String reqBody = "{\"CDADocumentType\":1001,\"CDAProfile\":{\"ProfileType\":0,\"VisitId\":[0]},\"Credentials\":{\"PrimeSuiteCredential\":{\"PrimeSuiteSiteId\":\"300242\",\"PrimeSuiteUserAlias\":\"\",\"PrimeSuiteUserName\":\"admin\",\"PrimeSuiteUserPassword\":\"password\"},\"VendorCredential\":{\"VendorLogin\":\"QATeamGuid\",\"VendorPassword\":\"QATeamGuid\"}},\"Header\":{\"DestinationSiteID\":\"300242\",\"PrimeSuiteUserID\":1,\"SourceSiteID\":\"\"},\"PatientID\":18191}";
		String reqBody = "{\"CDADocumentType\":1001,\"CDAProfile\":{\"ProfileType\":0,\"VisitId\":[0]},\"Credentials\":{\"PrimeSuiteCredential\":{\"PrimeSuiteSiteId\":\"300242\",\"PrimeSuiteUserAlias\":\"\",\"PrimeSuiteUserName\":\"admin\",\"PrimeSuiteUserPassword\":\"password\"},\"VendorCredential\":{\"VendorLogin\":\"QATeamGuid\",\"VendorPassword\":\"QATeamGuid\"}},\"Header\":{\"DestinationSiteID\":\"300242\",\"PrimeSuiteUserID\":1,\"SourceSiteID\":\"\"},\"PatientID\":"+PatientID +"}";
		
		Response entity = client.target("https://api-test.greenwaymedical.com/Integration/RESTv1.0/PrimeSuiteAPIService")
		//.register(FilterForExampleCom.class)
		.path("Patient/ClinicalSummaryGet")
        .queryParam("api_key", "gxzzfraga2uunq2mj6s79zdx")
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.json(reqBody));
        
		if (entity.getStatus()!= 200){
			return null;
		}
						
		String str = entity.readEntity(String.class);
		JSONObject json = null;
		String xml = null;
		//System.out.println(str);
		try {
			json = new JSONObject(str);
			xml = json.getString("Data");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		return xml;

	}

}
