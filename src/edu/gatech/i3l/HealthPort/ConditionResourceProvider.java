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

import org.eclipse.emf.common.util.EList;
import org.openhealthtools.mdht.uml.cda.EntryRelationship;
import org.openhealthtools.mdht.uml.cda.Observation;
import org.openhealthtools.mdht.uml.cda.ccd.CCDPackage;
import org.openhealthtools.mdht.uml.cda.ccd.ContinuityOfCareDocument;
import org.openhealthtools.mdht.uml.cda.ccd.ProblemAct;
import org.openhealthtools.mdht.uml.cda.ccd.ProblemSection;
import org.openhealthtools.mdht.uml.cda.util.CDAUtil;
import org.openhealthtools.mdht.uml.hl7.datatypes.CD;
import org.openhealthtools.mdht.uml.hl7.datatypes.CE;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.valueset.ConditionStatusEnum;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;


public class ConditionResourceProvider implements IResourceProvider {
	public static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	public static final String JDBC_URL = "jdbc:derby:" + Config.DBPath+"/MyDB;user=adh;password=123";
	public static final String SQL_STATEMENT = "select name from cdcAppDB.Users";

	@Override
	public Class<? extends IResource> getResourceType() {
		// TODO Auto-generated method stub
		return Condition.class;
	}

	@Search()
	public List<Condition> getConditionsByPatient(
			@RequiredParam(name=Condition.SP_SUBJECT) ReferenceParam theSubject) {

		Connection connection = null;
		Statement statement = null;
		String name=null;
		String location=null;
		String SQL_Statement1=null;
		String ccd=null;
		String gwId = null;
		String pid = theSubject.getIdPart();
		
		ArrayList<Condition> retVal = new ArrayList<Condition>();
		
    	int patientNum = Integer.parseInt(pid);
    	
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
	
    	//Access Greenway to get CCD for patient
    		if(location.equals("GW")){
    			ccd = GreenwayPort.getCCD(gwId);
    			System.out.println(ccd);
    		     	
    		}
		
    	// Access Healthvault to get CCD for patient and parse
    		 
    	if(location.equals("HV")){
    		System.out.println("I am in HV");
    			
    		ccd = HealthVaultPort.getCCD(name);
    			
    		System.out.println(ccd);
    		
    		//Parsing of CCD
    		
    		CCDPackage.eINSTANCE.eClass();
    		ContinuityOfCareDocument ccdDocument = null;
    		ArrayList<String> conditionList = new ArrayList<String>();

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
    	
    		ProblemSection problem = ccdDocument.getProblemSection();
    		if(problem!=null){
    		EList<ProblemAct> problemActs = problem.getProblemActs();
    		//System.out.println(problemActs.get(0).getText());
    		for(int i =0; i < problemActs.size(); i++){
    			EList<Observation> probOb = problemActs.get(i).getObservations();
    			//System.out.println(probOb.get(i).getStatusCode().getCode());
    			CD value = (CD) probOb.get(0).getValues().get(0); // DataType, CD from CCD file
    			//System.out.println(value.getDisplayName()); // This is to get display name: Proximal DVT
    			conditionList.add(value.getDisplayName());

    			// Status we need is from entryRelationship
    			for (EntryRelationship entryRel : probOb.get(0).getEntryRelationships()) {
    				CD codeV = entryRel.getObservation().getCode();
    				if (codeV.getCodeSystem().equals("2.16.840.1.113883.6.1")) {
    					if (codeV.getCode().equals("33999-4")) {
    						// Here we need to compare the status and map to FHIR status
    						// provisional
    						//This is a tentative diagnosis - still a candidate that is under consideration.
    						// working
    						//The patient is being treated on the basis that this is the condition, but it is still not confirmed.
    						// confirmed
    						//There is sufficient diagnostic and/or clinical evidence to treat this as a confirmed condition.
    						// refuted
    						//This condition has been ruled out by diagnostic and clinical evidence.
    						// For active, we use confirmed. 
    						// Snomed code for ProblemStatus can be obtained from
    						// https://art-decor.org/art-decor/decor-valuesets--ccd1-?valueSetRef=2.16.840.1.113883.1.11.20.13

    						CE statV = (CE) entryRel.getObservation().getValues().get(0);
    						//System.out.println ("code system: "+statV.getCodeSystem()+", code: "+statV.getCode()+", display: "+statV.getDisplayName());
    						conditionList.add(statV.getCodeSystem());
    						conditionList.add(statV.getCode());
    						
    					}
    				}
    			}
    		}
    		}

    		//create Condition
    		if (conditionList.size() ==0){
    			System.out.println("No condtion");
    		}

    		for (int i = 0; i < conditionList.size(); i=i+3) {
    			//String pid = theSubject.getIdPart();

    			Condition cond = new Condition();
    			cond.setId("pid:"+pid);
    			ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+pid);
    			cond.setSubject(subj);
    			CodeableConceptDt value = new CodeableConceptDt();
    			// We have to put system and code. This should be obtained from CCD. 
    			// If not found like coag CCD, then we don't put anything as this is 
    			// not a required field.
    			// Instead, we put text.
    			value.setText(conditionList.get(i));
    			cond.setCode(value );
    			//cond.setStatus(ConditionStatusEnum.CONFIRMED);
    			if (conditionList.get(i+1).equals("2.16.840.1.113883.1.11.20.13")) {
					if (conditionList.get(i+2).equals("55561003")) {
						// Active
						cond.setStatus(ConditionStatusEnum.CONFIRMED);
					} else if (conditionList.get(i+2).equals("73425007")) {
						// Inactive
						cond.setStatus(ConditionStatusEnum.REFUTED);
						//System.out.println("Put refuted to FHIR status");
					} else if (conditionList.get(i+2).equals("90734009")) {
						// Chronic
						cond.setStatus(ConditionStatusEnum.CONFIRMED);
						//System.out.println("Put confirmed to FHIR status");
					} else if (conditionList.get(i+2).equals("7087005")) {
						// Intermittent
						cond.setStatus(ConditionStatusEnum.WORKING);
						//System.out.println("Put working to FHIR status");
					} else if (conditionList.get(i+2).equals("255227004")) {
						// Recurrent
						cond.setStatus(ConditionStatusEnum.WORKING);
						//System.out.println("Put working to FHIR status");
					} else if (conditionList.get(i+2).equals("415684004")) {
						// Rule out
						cond.setStatus(ConditionStatusEnum.REFUTED);
						//System.out.println("Put refuted to FHIR status");
					} else if (conditionList.get(i+2).equals("410516002")) {
						// Ruled out
						cond.setStatus(ConditionStatusEnum.REFUTED);
						//System.out.println("Put refuted to FHIR status");
					} else if (conditionList.get(i+2).equals("413322009")) {
						// Resolved
						cond.setStatus(ConditionStatusEnum.CONFIRMED);
						//System.out.println("Put refuted to FHIR status");
					} 
				}

    			retVal.add(cond);
    		}
    	}
		
	
		return retVal;
	}
}