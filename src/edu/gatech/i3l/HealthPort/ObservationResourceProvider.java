package edu.gatech.i3l.HealthPort;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu.composite.QuantityDt;
import ca.uhn.fhir.model.dstu.resource.Observation;
import ca.uhn.fhir.model.dstu.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationReliabilityEnum;
import ca.uhn.fhir.model.dstu.valueset.ObservationStatusEnum;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

public class ObservationResourceProvider implements IResourceProvider {

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
		Context context = null;
		DataSource datasource = null;
		String name=null;
		String location=null;
		String ccd=null;	
		String output = null;
  
    	ArrayList<Observation> retVal = new ArrayList<Observation>();
    	ArrayList<String> retList = new ArrayList<String>();
    	String PatientID;
    	FhirContext ctx = new FhirContext();
 
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
		
    	HealthPortUserInfo HealthPortUser = new HealthPortUserInfo(patientNum);
    	String rId = HealthPortUser.recordId;
    	String pId = HealthPortUser.personId;
    	location = HealthPortUser.dataSource;
    	
//		try{
//			context = new InitialContext();
//			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/HealthPort");
//			connection = datasource.getConnection();
//			statement = connection.createStatement();
//			String SQL_STATEMENT = "SELECT U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID) WHERE U1.ID="+patientNum;
//			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
//			if (resultSet.next()) {
//				name = resultSet.getString("NAME");
//				location = resultSet.getString("TAG");
//				rId = resultSet.getString("RECORDID");
//				pId = resultSet.getString("PERSONID");
//				System.out.println(patientNum);
//				System.out.println(name+":"+location);
//			} else {
//				return retVal;
//			}
//			
//			connection.close();
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		//Access Greenway and get CCD of patient
		if(location.equals(HealthPortUserInfo.GREENWAY)){
			ccd = GreenwayPort.getCCD(pId);
			System.out.println(ccd);
	     	
		} else if (location.equals(HealthPortUserInfo.SyntheticEHR)) {
			retVal = new SyntheticEHRPort().getObservations(HealthPortUser);
			
		} else if(location.equals(HealthPortUserInfo.HEALTHVAULT)){
			// Access Healthvault to get Patient information
			//retVal = HVCCDParse(rId, pId,PatientID);
			
			//Get the Weight and create Observations
			retList = HealthVaultPort.getWeight(rId, pId);
			for (int i = 0; i < retList.size(); i=i+2) {
				Observation obs = new Observation();
			
				obs.setId("pid:"+PatientID); // This is object resource ID. 
				String nameCode = getCode("Body weight");
				obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
				QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i))).setUnits(retList.get(i+1));
				obs.setValue(quantity);
				obs.setComments("Weight");
				obs.setStatus(ObservationStatusEnum.FINAL);
				obs.setReliability(ObservationReliabilityEnum.OK);
				
				obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
				ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
			    obs.getText().setDiv(output);
			    
				retVal.add(obs);
			}
			//Get the Height and create Observations
			retList.clear();
			retList=(HealthVaultPort.getHeight(rId, pId));
			for (int i = 0; i < retList.size(); i=i+2) {
				Observation obs = new Observation();
				obs.setId("pid:"+PatientID); // This is object resource ID. 
				String nameCode = "0000";
				obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
				QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i))).setUnits(retList.get(i+1));
				obs.setValue(quantity);
				obs.setComments("Height");
				obs.setStatus(ObservationStatusEnum.FINAL);
				obs.setReliability(ObservationReliabilityEnum.OK);
				
				obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
				ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
			    obs.getText().setDiv(output);
			    
				retVal.add(obs);
			}
			//Get the blood Pressure and create Observations
			retList.clear();
			retList = HealthVaultPort.getBloodPressure(rId, pId);
			for (int i = 0; i < retList.size(); i=i+3) {
				Observation obs = new Observation();
				obs.setId("pid:"+PatientID); // This is object resource ID. 
				String nameCode = "0000";
				obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
				QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i+1))).setUnits(retList.get(i+2));
				obs.setValue(quantity);
				obs.setComments(retList.get(i));
				obs.setStatus(ObservationStatusEnum.FINAL);
				obs.setReliability(ObservationReliabilityEnum.OK);
				
				obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
				ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
			    obs.getText().setDiv(output);
			    
				retVal.add(obs);
			}
			//Get the Lab Results and create Observations
			retList.clear();
			retList = HealthVaultPort.getLabResults(rId, pId);
			for (int i = 0; i < retList.size(); i=i+4) {
				Observation obs = new Observation();
				obs.setId("pid:"+PatientID); // This is object resource ID. 
				String nameCode = "0000";
				obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
				QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i+2))).setUnits(retList.get(i+3));
				obs.setValue(quantity);
				obs.setComments(retList.get(i+1));
				obs.setStatus(ObservationStatusEnum.FINAL);
				obs.setReliability(ObservationReliabilityEnum.OK);
				
				obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
				ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
			    obs.getText().setDiv(output);
			    
				retVal.add(obs);
			}
			retList.clear();
			retList = HealthVaultPort.getBloodGlucose(rId, pId);
			for (int i = 0; i < retList.size(); i=i+3) {
				Observation obs = new Observation();
				obs.setId("pid:"+PatientID); // This is object resource ID. 
				String nameCode = "0000";
				obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
				QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i))).setUnits(retList.get(i+1));
				obs.setValue(quantity);
				obs.setComments("Glucose in " + retList.get(i+2));
				obs.setStatus(ObservationStatusEnum.FINAL);
				obs.setReliability(ObservationReliabilityEnum.OK);
				
				obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
				ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
			    obs.getText().setDiv(output);
			    
				retVal.add(obs);
			}
			retList.clear();
			retList = HealthVaultPort.getCholesterol(rId, pId);
			for (int i = 0; i < retList.size(); i=i+2) {
				Observation obs = new Observation();
				obs.setId("pid:"+PatientID); // This is object resource ID. 
				String nameCode = "0000";
				obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
				QuantityDt quantity = new QuantityDt(Double.parseDouble(retList.get(i))).setUnits(retList.get(i+1));
				obs.setValue(quantity);
				obs.setComments("Cholesterol");
				obs.setStatus(ObservationStatusEnum.FINAL);
				obs.setReliability(ObservationReliabilityEnum.OK);
				
				obs.getText().setStatus(NarrativeStatusEnum.GENERATED);
				ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(obs);
			    obs.getText().setDiv(output);
			    
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
    
    //Parse a given CCD (using mdht) and create observations
    public static ArrayList<Observation> HVCCDParse(String rId, String pId,String PatientID){
    	String ccd=null;
    	ArrayList<Observation> retVal = new ArrayList<Observation>();
    	//get CCD from healthVault
    	ccd = HealthVaultPort.getCCD(rId, pId);
		
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
		//Retrieve Results from CCD (lab tests)
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
		//Retrieve Vitals from CCD 
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
			obs.setName(new CodeableConceptDt("http://loinc.org",nameCode)); 
			QuantityDt quantity = new QuantityDt(Double.parseDouble(observationList.get(i+1))).setUnits(observationList.get(i+2));
			obs.setValue(quantity);
			obs.setComments(observationList.get(i));
			obs.setStatus(ObservationStatusEnum.FINAL);
			obs.setReliability(ObservationReliabilityEnum.OK);
			retVal.add(obs);
		}
    	return retVal;
    }
    
 
}