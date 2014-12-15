package edu.gatech.i3l.HealthPort;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.openhealthtools.mdht.uml.cda.ccd.CCDPackage;
import org.openhealthtools.mdht.uml.cda.ccd.ContinuityOfCareDocument;
import org.openhealthtools.mdht.uml.cda.ccd.ResultObservation;
import org.openhealthtools.mdht.uml.cda.ccd.ResultOrganizer;
import org.openhealthtools.mdht.uml.cda.ccd.ResultsSection;
import org.openhealthtools.mdht.uml.cda.ccd.VitalSignsOrganizer;
import org.openhealthtools.mdht.uml.cda.ccd.VitalSignsSection;
import org.openhealthtools.mdht.uml.cda.util.CDAUtil;
import org.openhealthtools.mdht.uml.hl7.datatypes.PQ;
import org.openhealthtools.mdht.uml.hl7.datatypes.ST;

import ca.uhn.fhir.model.dstu.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu.composite.QuantityDt;
import ca.uhn.fhir.model.dstu.resource.Observation;
//import ca.uhn.fhir.model.dstu.resource.Patient;
import ca.uhn.fhir.model.dstu.valueset.ObservationReliabilityEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationStatusEnum;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

public class ObservationResourceProvider implements IResourceProvider {
	public static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	public static final String JDBC_URL = "jdbc:derby:" + Config.DBPath+"/MyDB;user=adh;password=123";
	public static final String SQL_STATEMENT = "select name from cdcAppDB.Users";

    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    public Class<Observation> getResourceType() {
        return Observation.class;
    }
     
    
    @Search()
    public List<Observation> getObservationsbyPatient(
    		@RequiredParam(name=Observation.SP_SUBJECT) ReferenceParam theSubject){
    	
    	Connection connection = null;
		Statement statement = null;
		String name=null;
		String location=null;
		String SQL_Statement1=null;
		String ccd=null;
		String gwId = null;
		
  
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

		
		try{
			Class.forName(DRIVER);
			connection = DriverManager.getConnection(JDBC_URL);
			statement = connection.createStatement();
			SQL_Statement1 = "select * from cdcAppDB.Users where ID = " + patientNum;
			ResultSet resultSet = statement.executeQuery(SQL_Statement1);
			if (resultSet.next()) {
				name = resultSet.getString("name");
				location = resultSet.getString("location");
				System.out.println(patientNum);
				System.out.println(name+":"+location);
			}
			
			if(location.equals("GW")){
				SQL_Statement1 = "select personId from cdcAppDB.GWUSERS where name = '" + name+ "'";
				resultSet = statement.executeQuery(SQL_Statement1);
				if (resultSet.next()) {
					gwId = resultSet.getString("personId");
				}
			}
			connection.close();

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try{
				if(statement != null) statement.close();
				if(connection != null) connection.close();
			}catch (SQLException e){e.printStackTrace();}
		}
		
		
		//Access Greenway and get CCD of patient
		if(location.equals("GW")){
			ccd = GreenwayPort.getCCD(gwId);
			System.out.println(ccd);
	     	
		}
			
		
		// Access Healthvault to get CCD of patient and parse
 
		if(location.equals("HV")){
			
			ccd = HealthVaultPort.getCCD(name);
			//System.out.println("In HV");
			
		//Parsing of CCD
		
		CCDPackage.eINSTANCE.eClass();
		ContinuityOfCareDocument ccdDocument = null;
		ArrayList<String> observationList = new ArrayList<String>();

		try {
		InputStream is = new ByteArrayInputStream(ccd.getBytes());
		ccdDocument = (ContinuityOfCareDocument) CDAUtil.load(is);
		} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		}
		
		

		ResultsSection results = ccdDocument.getResultsSection();
		if(results!=null){
		for (ResultOrganizer resultOrganizer : results.getResultOrganizers()) {
			for (ResultObservation resultObservation : resultOrganizer.getResultObservations()) {
				observationList.add(resultObservation.getCode().getOriginalText().getText());
				if (!resultObservation.getValues().isEmpty() && resultObservation.getValues().get(0) instanceof PQ) {
					PQ value = (PQ) resultObservation.getValues().get(0);
					observationList.add((value.getValue()).toString());
					observationList.add(value.getUnit());
				}
				if (!resultObservation.getValues().isEmpty() && resultObservation.getValues().get(0) instanceof ST) {
					ST value = (ST) resultObservation.getValues().get(0);
					observationList.add(value.getText());
					observationList.add("N/A");
				}
			}
		}
		}
		VitalSignsSection vitals = ccdDocument.getVitalSignsSection();
		if(vitals!=null){
		for (VitalSignsOrganizer vitalsOrganizer : vitals.getVitalSignsOrganizers()) {
			for (ResultObservation resultObservation : vitalsOrganizer.getResultObservations()) {
				observationList.add(resultObservation.getCode().getDisplayName());
				if (!resultObservation.getValues().isEmpty() && resultObservation.getValues().get(0) instanceof PQ) {
					PQ value = (PQ) resultObservation.getValues().get(0);
					observationList.add(value.getValue().toString());
					observationList.add(value.getUnit());
				}
				
			}
		}
		}
		//create Observations
		
		for (int i = 0; i < observationList.size(); i=i+3) {
			Observation obs = new Observation();
		
			obs.setId("pid:"+PatientID); // This is object resource ID. 
			String nameCode = getCode(observationList.get(i));
			//System.out.println(nameCode);
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(observationList.get(i+1))).setUnits(observationList.get(i+2));
			obs.setValue(quantity);
			obs.setComments(observationList.get(i));
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);

			retVal.add(obs);
		}
		}

       return retVal;

    }
    public static String getCode(String name) {
	    String lcode = null;
    	if (name.equals("Blood Urea Nitrogen")){
    		lcode = "49071-4";
    	}
    	if (name.equals("Creatinine Test")){
    		lcode = "30004-6";
    	}
    	if (name.equals("PT (Prothrombin Time)")){
    		lcode = "5894-1";
    	}
    	if (name.equals("INR (International Normalized Ratio)")){
    		lcode = "72281-9";
    	}
    	if (name.equals("Glomerular Filtration Rate (GFR)")){
    		lcode = "69405-9";
    	}
    	if (name.equals("Hemoglobin A1C (HbA1C)")){
    		lcode = "55454-3";
    	}
    	if (name.equals("Compression ultrasonography")){
    		lcode = "000";
    	}
    	if (name.equals("CAT Scan")){
    		lcode = "35884-6";
    	}
    	if (name.equals("Body weight")){
    		lcode = "3141-9";
    	}
    	
  	
    	
    	return lcode;
	}
    
    
    
    
    
    
    
    
    
    
 
}