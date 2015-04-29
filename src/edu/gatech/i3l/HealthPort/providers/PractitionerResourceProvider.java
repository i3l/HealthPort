/**
 * 
 */
package edu.gatech.i3l.HealthPort.providers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu.resource.Patient;
import ca.uhn.fhir.model.dstu.resource.Practitioner;
import ca.uhn.fhir.model.dstu.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import edu.gatech.i3l.HealthPort.HealthPortInfo;
import edu.gatech.i3l.HealthPort.PractitionerPortInfo;
import edu.gatech.i3l.HealthPort.ports.GreenwayPort;
import edu.gatech.i3l.HealthPort.ports.HealthVaultPort;

/**
 * @author Paul
 *
 */

public class PractitionerResourceProvider implements IResourceProvider {

	/* (non-Javadoc)
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */

	public static final String SQL_STATEMENT = "SELECT * FROM provider"; 
	String SQL_STATEMENT_GET_PROVIDERS_BY_PATIENT = "select enc.Provider_ID, enc.Member_ID, enc.Provider_Name from encounter as enc LEFT JOIN provider AS pro ON(pro.Provider_ID=enc.Provider_ID and pro.Name = enc.Provider_Name) where enc.Member_ID=?";
	
	 
	private PractitionerPortInfo practitionerPortInfo;
	private HealthPortInfo healthPortUser;
	

	private String GWID;
	private String HVID;
	
	public PractitionerResourceProvider() {
		practitionerPortInfo = new PractitionerPortInfo ("jdbc/HealthPort");
		healthPortUser = new HealthPortInfo ("jdbc/HealthPort");
		GWID = healthPortUser.getOrgID(GreenwayPort.GREENWAY);
		HVID = healthPortUser.getOrgID(HealthVaultPort.HEALTHVAULT);
	
	}

	public Class<Practitioner> getResourceType() {
		return Practitioner.class;
	}


	@Read()
	public Practitioner getResourceById(@IdParam IdDt theId){

		Practitioner practitioner = new Practitioner();
		if(theId==null) return practitioner;

		String providerId;
		String id = theId.getIdPart();

		practitionerPortInfo.setInformation(id);
		providerId = practitionerPortInfo.providerId;

		practitioner.addIdentifier();
		practitioner.getIdentifier().get(0).setSystem(new UriDt("urn:healthport:mrns"));
		practitioner.setId(providerId);
		practitioner.getIdentifier().get(0).setValue(providerId);

		String fullName = null;
		
		if(practitionerPortInfo.name!=null) {
			
			String[] userName  = practitionerPortInfo.name.split(" ");
			if (userName.length == 2){
				
				String family = userName[1].replaceAll(",", "");
				practitioner.getName().addFamily(family);
				practitioner.getName().addGiven(userName[0]);
				fullName = userName[0] + " "+ family;
			}
			else{
				String family = userName[2].replaceAll(",", "");
				practitioner.getName().addFamily(family);
				practitioner.getName().addGiven(userName[0]+ " "+ userName[1]);
				fullName = userName[0] + " "+userName[1]+ " "+ family;

			}

			practitioner.setOrganization(new ResourceReferenceDt(practitionerPortInfo.providerOrg));
			practitioner.getLocation().add(new ResourceReferenceDt(practitionerPortInfo.location));

			practitioner.getText().setStatus(NarrativeStatusEnum.GENERATED);
			String textBody = "<table>"
					+ "<tr><td>Name</td><td>"+ fullName
					+ "</td></tr></table>";
			practitioner.getText().setDiv(textBody);

		}
		return practitioner;
	}


	@Search
	public List<Practitioner> getAllPractitioners() {

		Connection connection = null;

		ArrayList<Practitioner> retVal = new ArrayList<Practitioner>();

		try {
			connection = practitionerPortInfo.getConnection();

			PreparedStatement pstmt = connection
					.prepareStatement(SQL_STATEMENT);

			ResultSet resultSet = pstmt.executeQuery();

			while (resultSet.next()) {

				practitionerPortInfo.setRSInformation(resultSet);
				if(practitionerPortInfo.name!=null) {

					String[] userName  = practitionerPortInfo.name.split(" ");

					Practitioner practitioner = new Practitioner();
					practitioner.addIdentifier();
					practitioner.getIdentifier().get(0).setSystem(new UriDt("urn:healthport:mrns"));

					String providerId;
					providerId = practitionerPortInfo.providerId;

					practitioner.setId(providerId);
					practitioner.getIdentifier().get(0).setValue(providerId);

					String fullName = null;
					if (userName.length == 2){
						String family = userName[1].replaceAll(",", "");
						practitioner.getName().addFamily(family);
						practitioner.getName().addGiven(userName[0]);
						fullName = userName[0] + " "+ family;
					}
					else{
						String family = userName[2].replaceAll(",", "");
						practitioner.getName().addFamily(family);
						practitioner.getName().addGiven(userName[0]+ " "+ userName[1]);
						fullName = userName[0] + " "+userName[1]+ " "+ family;
					}

					practitioner.setOrganization(new ResourceReferenceDt(practitionerPortInfo.providerOrg));
					practitioner.getLocation().add(new ResourceReferenceDt(practitionerPortInfo.location));

					practitioner.getText().setStatus(NarrativeStatusEnum.GENERATED);
					String textBody = "<table>"
							+ "<tr><td>Name</td><td>"+ fullName
							+ "</td></tr></table>";
					practitioner.getText().setDiv(textBody);

					retVal.add(practitioner);
				}
			}
			connection.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		return retVal;

	}

	 @Search(queryName="patient")
	    //http://localhost:8080/HealthPort/fhir/Provider?_query=patient&identifier=3.887654321
	    public IBundleProvider getAllProvidersByPatient( @RequiredParam(name = Patient.SP_IDENTIFIER) ReferenceParam patient) {
	    			
			final InstantDt searchTime = InstantDt.withCurrentTime();
			if(patient==null) return new IBundleProvider() {

				@Override
				public List<IResource> getResources(int theFromIndex, int theToIndex) {
					// TODO Auto-generated method stub
					List<IResource> retVal = new ArrayList<IResource>();
					return retVal;
				}

				@Override
				public int size() {
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public InstantDt getPublished() {
					// TODO Auto-generated method stub
					return searchTime;
				}
				
				
			};
			
			String patientId = patient.getIdPart();
	 
			final List<IResource> providers  = getProviders(patientId);
			
			
			return new IBundleProvider() {

				@Override
				public int size() {
					return providers.size();
				}

				@Override
				public List<IResource> getResources(int theFromIndex, int theToIndex) {
					
					List<IResource> retVal = null;

					retVal = providers.subList(theFromIndex, theToIndex);
					
					return retVal;
				}

				@Override
				public InstantDt getPublished() {
					return searchTime;
				}
			};

	    }


		private List<IResource> getProviders(String memberId) {
			Connection connection;
			
			List<IResource> retVal = new ArrayList<IResource>();
			String patientID;
			
			String Ids[] = memberId.split(".");
			if(Ids.length==2) {
				patientID = Ids[1];
			}
			else {
				patientID = memberId;
			}
	    	
	    	try{
	    		connection = practitionerPortInfo.getConnection();
				
				PreparedStatement pstmt = connection
						.prepareStatement(SQL_STATEMENT_GET_PROVIDERS_BY_PATIENT);
				pstmt.setString(1, patientID);

				ResultSet resultSet = pstmt.executeQuery();
				
				while (resultSet.next()) {
					
			
					String providerName, providerId;
					
					providerId = resultSet.getString("Provider_ID");
					providerName = resultSet.getString("Provider_Name");
					
					String[] userName  = providerName.split(" ");
					
					Practitioner practitioner = new Practitioner();
								
					practitioner.addIdentifier();
					practitioner.getIdentifier().get(0).setSystem(new UriDt("urn:healthport:mrns"));

					practitioner.setId(providerId);
					practitioner.getIdentifier().get(0).setValue(providerId);

					String fullName = null;
					if (userName.length == 2){
						String family = userName[1].replaceAll(",", "");
						practitioner.getName().addFamily(family);
						practitioner.getName().addGiven(userName[0]);
						fullName = userName[0] + " "+ family;
					}
					else{
						String family = userName[2].replaceAll(",", "");
						practitioner.getName().addFamily(family);
						practitioner.getName().addGiven(userName[0]+ " "+ userName[1]);
						fullName = userName[0] + " "+userName[1]+ " "+ family;
					}

					practitioner.getText().setStatus(NarrativeStatusEnum.GENERATED);
					String textBody = "<table>"
							+ "<tr><td>Name</td><td>"+ fullName
							+ "</td></tr></table>";
					practitioner.getText().setDiv(textBody);

					retVal.add(practitioner);
				}
				connection.close();
				
			} catch (Exception e){
				e.printStackTrace();
			}
			return retVal;
		}
	 

}
