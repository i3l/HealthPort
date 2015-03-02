/**
 * 
 */
package edu.gatech.i3l.HealthPort.ports;

//import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;

import edu.gatech.i3l.HealthPort.HealthPortInfo;
import edu.gatech.i3l.HealthPort.PortIf;

/**
 * @author Neha
 *
 */
public class GreenwayPort implements PortIf {
	public static String GREENWAY = "GW";
	
	String tag;
	String id;
	
	/**
	 * 
	 */
	public GreenwayPort() {
		this.tag = GREENWAY;
		try {
			this.id = HealthPortInfo.findIdFromTag(tag);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
	public ArrayList<String> getObsIds() {
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.gatech.i3l.HealthPort.PortIf#getTag()
	 */
	@Override
	public String getTag() {
		// TODO Auto-generated method stub
		return tag;
	}

	/* (non-Javadoc)
	 * @see edu.gatech.i3l.HealthPort.PortIf#getId()
	 */
	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return id;
	}

}
