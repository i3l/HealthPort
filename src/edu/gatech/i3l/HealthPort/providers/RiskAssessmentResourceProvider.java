/**
 * 
 */
package edu.gatech.i3l.HealthPort.providers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dev.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dev.resource.Condition;
import ca.uhn.fhir.model.dev.resource.Observation;
import ca.uhn.fhir.model.dev.resource.OperationOutcome;
import ca.uhn.fhir.model.dev.resource.Patient;
import ca.uhn.fhir.model.dev.resource.RiskAssessment;
import ca.uhn.fhir.model.dev.valueset.IssueSeverityEnum;
import ca.uhn.fhir.model.dstu.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.primitive.DecimalDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

/**
 * @author MC142
 *
 */
public class RiskAssessmentResourceProvider implements IResourceProvider {
	//public static final String predictModel = "/Users/ameliahenderson/Desktop/predict.py";
	//public static final String patientFilePath = "/Users/ameliahenderson/Desktop/";
	public static final String predictModel = "/home/localadmin/dev/predictive_system/predict_mortality.py";
	public static final String patientFilePath = "/home/localadmin/dev/predictive_system/data/";
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */
	
	private Context context;
	private DataSource datasource;
	
	public RiskAssessmentResourceProvider () {
		try {
			context = new InitialContext();
			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/HealthPort");
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public Class<RiskAssessment> getResourceType() {
		// TODO Auto-generated method stub
		return RiskAssessment.class;
	}
	@Read()
	public RiskAssessment getResourceById(@IdParam IdDt theId) {
		RiskAssessment risk = new RiskAssessment();
		String resourceId = theId.getIdPart();
		System.out.println(resourceId);
		
		int count = StringUtils.countMatches(resourceId, "-");
		int numIds = count +1;
		
		String[] Ids = theId.getIdPart().split("\\-", numIds);
		System.out.println(Ids[0]);
		
		
		
		
		return risk;
		
	}
	/*@Read()
	public IBundleProvider getResourceById(@IdParam IdDt theId) {
		RiskAssessment risk = new RiskAssessment();
		String resourceId = theId.getIdPart();
		System.out.println(resourceId);
		
		int count = StringUtils.countMatches(resourceId, "-");
		int numIds = count +1;
		
		String[] Ids = theId.getIdPart().split("\\-", numIds);
		//System.out.println(Ids[0]);
		final InstantDt searchTime = InstantDt.withCurrentTime();
        final List<String> matchingResourceIds = Arrays.asList(Ids);
 
        return new IBundleProvider() {
 
            @Override
            public int size() {
                return matchingResourceIds.size();
            }
 
            @Override
            public List<IResource> getResources(int theFromIndex, int theToIndex) {
                int end = Math.max(theToIndex, matchingResourceIds.size() - 1);
                List<String> idsToReturn = matchingResourceIds.subList(theFromIndex, end);
                return loadResourcesByIds(idsToReturn);
            }
 
            @Override
            public InstantDt getPublished() {
                return searchTime;
            }
        };
		
	}*/
	
	
	
	
	@Search()
	public List<RiskAssessment> getRiskAssessmentbyID(
			@RequiredParam(name = RiskAssessment.SP_SUBJECT) ReferenceParam theSubject) {
		ArrayList<RiskAssessment> retV = new ArrayList<RiskAssessment>();
		String groupId = null;
		
		if (theSubject.hasResourceType()) {
			String resourceType = theSubject.getResourceType();
			if ("Group".equals(resourceType) == false) {
				throw new InvalidRequestException(
						"Invalid resource type for parameter 'subject': "
								+ resourceType);
			} else {
				groupId = theSubject.getIdPart();
			}
		} else {
			throw new InvalidRequestException(
					"Need resource type for parameter 'subject'");
		}
		//String resourceId = theSubject.getValue();
		//System.out.println(resourceId);
		
		//int count = StringUtils.countMatches(resourceId, "-");
		//int numIds = count +1;
		String patientId = null;
		Double score = null;
		Double runtime = null;
		String method = null;
		String dataSource = null;
		Double featConstructRuntime = null;
		
		
		//String[] Ids = theID.getValue().split("\\-", numIds);
		//System.out.println(Ids[0]);
		
		//DataSource datasource = null;
		Connection connection = null;
		Statement statement = null;
		//Context context = null;
		
		
		/*String SQL_Count = "SELECT COUNT(*) FROM RISKASSESSMENT WHERE GROUPID='"+ groupId +"'";
		ResultSet check_ret = statement.executeQuery(SQL_Count);
		check_ret.next();
		int numIds = check_ret.getInt(1);*/
		
		try {
//			context = new InitialContext();
//			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/HealthPort");
			connection = datasource.getConnection();
			statement = connection.createStatement();
			String SQL_Count = "SELECT PATIENTID, SCORE, RUNTIME, METHOD, DATASOURCE, FCRUNTIME FROM RISKASSESSMENT WHERE GROUPID='"+ groupId +"'";
			ResultSet check_ret = statement.executeQuery(SQL_Count);
			while (check_ret.next()) {
				RiskAssessment risk = new RiskAssessment();
				
				patientId = check_ret.getString(1);
				score = check_ret.getDouble(2);
				runtime = check_ret.getDouble(3);
				method = check_ret.getString(4);
				dataSource = check_ret.getString(5);
				featConstructRuntime = check_ret.getDouble(6);
				
				risk.setId(patientId);
				ResourceReferenceDt subj = new ResourceReferenceDt("Group/"+groupId);
				risk.setSubject(subj);
				String databaseName = null;
				if(dataSource.equals("ExactData")){
					databaseName = "omop_v4_exactdata";
				}
				if(dataSource.equals("MIMIC2")){
					databaseName = "omop_v4_mimic2";
				}
				
				CodeableConceptDt methodInformation = new CodeableConceptDt();
				methodInformation.setText(method+","+dataSource+","+databaseName);
				risk.setMethod(methodInformation);
				
				
				DecimalDt dec = new DecimalDt();
				dec.setValue(BigDecimal.valueOf(score));
				
				IDatatype theValue = null;
				risk.addPrediction();
				risk.getPrediction().get(0).setProbability(dec);
				risk.getPrediction().get(0).setRationale("runtime = "+runtime.toString() + " ," + "feature construction runtime = "+featConstructRuntime);

				retV.add(risk);
				
			}
			connection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {
				connection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		/*for(int i=0; i < numIds; i++){
			RiskAssessment risk = new RiskAssessment();
			try {
				context = new InitialContext();
				datasource = (DataSource) context.lookup("java:/comp/env/jdbc/HealthPort");
				connection = datasource.getConnection();
				statement = connection.createStatement();
				String SQL_Count2 = "SELECT PATIENTID, SCORE, RUNTIME, METHOD, DATASOURCE FROM RISKASSESSMENT WHERE GROUPID='"+ groupId +"'";
				ResultSet check_ret2 = statement.executeQuery(SQL_Count);
				check_ret.next();
				patientId = check_ret.getString(1);
				score = check_ret.getDouble(2);
				runtime = check_ret.getDouble(3);
				method = check_ret.getString(4);
				dataSource = check_ret.getString(5);
				//score = Double.toString(tempScore);
				
			} catch (NamingException | SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			risk.setId(Ids[i]);
			ResourceReferenceDt subj = new ResourceReferenceDt("Patient/"+patientId);
			risk.setSubject(subj);
			String databaseName = null;
			if(dataSource.equals("ExactData")){
				databaseName = "omop_v4_exactdata";
			}
			if(dataSource.equals("MIMIC2")){
				databaseName = "omop_v4_mimic2";
			}
			
			CodeableConceptDt methodInformation = new CodeableConceptDt();
			methodInformation.setText(method+","+dataSource+","+databaseName);
			risk.setMethod(methodInformation);
			
			
			DecimalDt dec = new DecimalDt();
			dec.setValue(BigDecimal.valueOf(score));
			
			IDatatype theValue = null;
			risk.addPrediction();
			risk.getPrediction().get(0).setProbability(dec);
			risk.getPrediction().get(0).setRationale("runtime = "+runtime.toString());
			
			
			//risk.getPrediction().get(0).setProbability(String.format("%.1f", new BigDecimal(tempScore)));
			retV.add(risk);
			}
			*/

		
		
		
		return retV;
		
	}
/*
	@Search()
	public List<RiskAssessment> getRiskAssessmentbySubject(
			@RequiredParam(name = Condition.SP_SUBJECT) ReferenceParam theSubject) {
		ArrayList<RiskAssessment> retV = null;
		return retV;
		
	}*/
	
	
	@Create()
	public MethodOutcome createRiskAssessment(@ResourceParam RiskAssessment theRisk){
		validateResource(theRisk);
		
		DecimalFormat newFormat = new DecimalFormat("#.###");
		DecimalFormat newFormat2 = new DecimalFormat("#.##");
//		DataSource datasource = null;
		Connection connection = null;
		Statement statement = null;
//		Context context = null;
		ArrayList<Integer> idList = new ArrayList<Integer>();

		int size = theRisk.getBasis().size();
		String PatientId = null;
		int count = 0;
		String method = theRisk.getMethod().getText();
		String patientFile = null;
		
		String[] methodInfo = method.split(",");
		String algorithmName = methodInfo[0];
		String dataSet = methodInfo[1];
		String groupId = theRisk.getSubject().getReference().getIdPart();
		

		BufferedWriter out = null;
		try  
		{
			patientFile = patientFilePath + UUID.randomUUID().toString() +".txt";
		    FileWriter fstream = new FileWriter(patientFile, false); //true tells to append data.
		    out = new BufferedWriter(fstream);
		    //method = theRisk.getMethod().getText();
		    out.write(method);
		    out.write("\n");
			
		    while(count !=size){
		    	PatientId = theRisk.getBasis().get(count).getReference().getIdPart();
		    	out.write(PatientId);
		    	out.write("\n");
		    	count = count+1;
		    }
		}
		catch (IOException e)
		{
		    System.err.println("Error: " + e.getMessage());
		}
		finally
		{
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("patient Ids written to file");
		Process p = null;
		int ret = 0;
		StringBuilder sb = new StringBuilder();
		try {
			System.out.println(patientFile);
			String pbCommand[] = { "python", predictModel, patientFile};
			//String pbCommand[] = { "python", predictModel," "+tempNum};
			System.out.println(pbCommand[0]+pbCommand[1]+pbCommand[2]);
			System.out.println("Running the python script");
			Process pb = Runtime.getRuntime().exec(pbCommand);
			 BufferedReader stdInput = new BufferedReader(new InputStreamReader(pb.getInputStream()));
			 //BufferedReader stdError = new BufferedReader(new InputStreamReader(pb.getErrorStream()));
			// read the output

	            String s;
				while ((s = stdInput.readLine()) != null) {
					sb.append(s);
	            }
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Script ran");
		System.out.println(sb);
		String finalString = sb.toString();
		
		JSONObject obj;
		JSONObject objPatient = null;
		Double objRuntime = null;
		Double featConstrRuntime = null;
		try {
			obj = new JSONObject(finalString);
			JSONArray arr = obj.getJSONArray("prediction");
			objRuntime = obj.getDouble("runtime");
			objRuntime = Double.valueOf(newFormat.format(objRuntime));
			featConstrRuntime = obj.getDouble("feature_construction_runtime");
			featConstrRuntime = Double.valueOf(newFormat.format(featConstrRuntime));
			System.out.println(objRuntime);
			System.out.println(featConstrRuntime);
			
			//System.out.println(objRuntime);
			for (int i = 0; i < arr.length(); i++){
				objPatient = new JSONObject(arr.getString(i));
				String personId = objPatient.getString("person_id");
				Double score = objPatient.getDouble("score");
				score = Double.valueOf(newFormat2.format(score));
				
//				context = new InitialContext();
//				datasource = (DataSource) context.lookup("java:/comp/env/jdbc/HealthPort");
				connection = datasource.getConnection();
				statement = connection.createStatement();
				String SQL_Count = "SELECT COUNT(*) FROM RISKASSESSMENT WHERE PATIENTID='"+ personId +"' AND SCORE='"+score +"' AND RUNTIME ='"+objRuntime+"' AND METHOD='"+algorithmName+"' AND DATASOURCE='"+dataSet+"' AND GROUPID='"+groupId+"' AND FCRUNTIME='"+featConstrRuntime+"'";
				ResultSet check_ret = statement.executeQuery(SQL_Count);
				check_ret.next();
				//System.out.println(objRuntime);
				String SQL_Statement = null;
				if (check_ret.getInt(1) == 0) {
					SQL_Statement = "INSERT INTO RISKASSESSMENT (PATIENTID, SCORE, RUNTIME, METHOD, DATASOURCE, GROUPID, FCRUNTIME) VALUES ('"+personId+ "', '"+score+"', '"+objRuntime+"', '"+algorithmName+"', '"+dataSet+"', '"+groupId+"', '"+featConstrRuntime+"')";
					statement.executeUpdate(SQL_Statement);
				} 
			
				connection.close();
				//statement.executeUpdate(SQL_Statement);
				
				/*SQL_Statement = "SELECT GROUPID FROM RISKASSESSMENT WHERE PATIENTID='"+ personId +"' AND SCORE='"+score+"' AND RUNTIME='"+objRuntime+"' AND METHOD='"+algorithmName+"' AND DATASOURCE='"+dataSet+"'";
				check_ret = statement.executeQuery(SQL_Statement);
				check_ret.next();
				//System.out.println(check_ret.getInt(1));
				idList.add(check_ret.getInt(1));*/
				
				
			    //System.out.println(personId);
				//System.out.println(score);
			}

		} catch (JSONException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		/*String finalId = idList.get(0).toString();
		int sizeId = idList.size();
		for (int j =1; j <sizeId;j++ ){
			finalId = finalId + "-"+idList.get(j).toString();
		}*/
		
		String finalId = groupId;
		System.out.println(finalId);
		return new MethodOutcome(new IdDt(finalId));
		
	}
	
	private void validateResource(RiskAssessment theRisk) {
		/*
		 * Our server will have a rule that patients must have a family name or we will reject them
		 */
		if (theRisk.getBasis().isEmpty()) {
			OperationOutcome outcome = new OperationOutcome();
			outcome.addIssue().setSeverity(IssueSeverityEnum.FATAL).setDetails("No basis provided,RiskAssessment resources must have basis.");
			throw new UnprocessableEntityException(outcome);
		}
	}

	 
	    /**
	     * Load a list of patient resources given their IDs
	     */
	    private List<IResource> loadResourcesByIds(List<String> theFamily) {
	        // .. implement this search against the database ..
	    	System.out.println(theFamily);
	        return null;
	    }
	 
	
	
}
