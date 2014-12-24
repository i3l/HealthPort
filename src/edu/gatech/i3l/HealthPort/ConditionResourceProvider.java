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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.valueset.ConditionStatusEnum;
import ca.uhn.fhir.model.dstu.valueset.NarrativeStatusEnum;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;


public class ConditionResourceProvider implements IResourceProvider {

	@Override
	public Class<? extends IResource> getResourceType() {
		// TODO Auto-generated method stub
		return Condition.class;
	}

	@Search()
	public List<Condition> getConditionsByPatient(
			@RequiredParam(name=Condition.SP_SUBJECT) ReferenceParam theSubject) {

		Connection connection = null;
		Context context = null;
		DataSource datasource = null;
		Statement statement = null;
		String name=null;
		String location=null;
		String ccd=null;
		String rId = null;
		String pId = null;
		FhirContext ctx = new FhirContext();
		String output = null;
    	int patientNum = Integer.parseInt(theSubject.getIdPart());
		
		ArrayList<Condition> retVal = new ArrayList<Condition>();    	
    	try{
    		context = new InitialContext();
    		datasource = (DataSource) context.lookup("java:/comp/env/jdbc/HealthPort");
    		connection = datasource.getConnection();
			statement = connection.createStatement();
			String SQL_STATEMENT = "SELECT U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID) WHERE U1.ID="+patientNum;
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			if (resultSet.next()) {
				name = resultSet.getString("NAME");
				location = resultSet.getString("TAG");
				rId = resultSet.getString("RECORDID");
				pId = resultSet.getString("PERSONID");
				System.out.println(patientNum);
				System.out.println(name+":"+location);
			} else {
				return retVal;
			}			
			connection.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
    	//Access Greenway to get CCD for patient
    	if(location.equals("GW")){
    			ccd = GreenwayPort.getCCD(pId);
    			System.out.println(ccd);
    	}
		
    	// Access Healthvault to get Patient information 
    	if(location.equals("HV")){ 			
    		ArrayList<String> conditionList = new ArrayList<String>();
    		
    		//retVal = HVCCDParseCondition(rId, pId,patientNum);
    		conditionList = HealthVaultPort.getCondition(rId, pId);
    		for (int i = 0; i < conditionList.size(); i=i+2) {
    			Condition cond = new Condition();
    			cond.setId("pid:"+patientNum);
    			ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+patientNum);
    			cond.setSubject(subj);
    			CodeableConceptDt value = new CodeableConceptDt();
    			value.setText(conditionList.get(i));
    			cond.setCode(value );
    			if (conditionList.get(i+1).equals("active")) {
					// Active
					cond.setStatus(ConditionStatusEnum.CONFIRMED);
				} else if (conditionList.get(i+1).equals("inactive")) {
					// Inactive
					cond.setStatus(ConditionStatusEnum.REFUTED);
					//System.out.println("Put refuted to FHIR status");
				} else if (conditionList.get(i+1).equals("chronic")) {
					// Chronic
					cond.setStatus(ConditionStatusEnum.CONFIRMED);
					//System.out.println("Put confirmed to FHIR status");
				} else if (conditionList.get(i+1).equals("intermittent")) {
					// Intermittent
					cond.setStatus(ConditionStatusEnum.WORKING);
					//System.out.println("Put working to FHIR status");
				} else if (conditionList.get(i+2).equals("recurrent")) {
					// Recurrent
					cond.setStatus(ConditionStatusEnum.WORKING);
					//System.out.println("Put working to FHIR status");
				} else if (conditionList.get(i+2).equals("rule out")) {
					// Rule out
					cond.setStatus(ConditionStatusEnum.REFUTED);
					//System.out.println("Put refuted to FHIR status");
				} else if (conditionList.get(i+2).equals("ruled out")) {
					// Ruled out
					cond.setStatus(ConditionStatusEnum.REFUTED);
					//System.out.println("Put refuted to FHIR status");
				} else if (conditionList.get(i+2).equals("resolved")) {
					// Resolved
					cond.setStatus(ConditionStatusEnum.CONFIRMED);
					//System.out.println("Put refuted to FHIR status");
				} 
    			
    			cond.getText().setStatus(NarrativeStatusEnum.GENERATED);
				ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
			    output = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(cond);
			    cond.getText().setDiv(output);
    			
    			retVal.add(cond);
    		}
    		
 
    	}
		
		return retVal;
	}
	//If given a CCD, parse and create Conditions
	 public static ArrayList<Condition> HVCCDParseCondition(String rId, String pId,int patientNum){
		 ArrayList<String> conditionList = new ArrayList<String>();
		 ArrayList<Condition> retVal = new ArrayList<Condition>(); 
		 String ccd=null;
		 ccd = HealthVaultPort.getCCD(rId, pId);
			
 		//Parsing of CCD
 		CCDPackage.eINSTANCE.eClass();
 		ContinuityOfCareDocument ccdDocument = null;
 		
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

 		for(int i =0; i < problemActs.size(); i++){
 			EList<Observation> probOb = problemActs.get(i).getObservations();
 			CD value = (CD) probOb.get(0).getValues().get(0); // DataType, CD from CCD file
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
	   		for (int i = 0; i < conditionList.size(); i=i+3) {
    			Condition cond = new Condition();
    			cond.setId("pid:"+patientNum);
    			ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+patientNum);
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
	   		return retVal;
		 
		 
	 }
}