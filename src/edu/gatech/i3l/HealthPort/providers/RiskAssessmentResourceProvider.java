/**
 * 
 */
package edu.gatech.i3l.HealthPort.providers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

















import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dev.resource.Condition;
import ca.uhn.fhir.model.dev.resource.Observation;
import ca.uhn.fhir.model.dev.resource.OperationOutcome;
import ca.uhn.fhir.model.dev.resource.Patient;
import ca.uhn.fhir.model.dev.resource.RiskAssessment;
import ca.uhn.fhir.model.dev.valueset.IssueSeverityEnum;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

/**
 * @author MC142
 *
 */
public class RiskAssessmentResourceProvider implements IResourceProvider {
	public static final String predictModel = "/Users/ameliahenderson/Desktop/predict.py";
	public static final String patientFile = "/Users/ameliahenderson/Desktop/persons_id.txt";
	//public static final String predictModel = "/home/localadmin/dev/predictive_system/predict_mortality.py";
	//public static final String patientFile = "/home/localadmin/dev/predictive_system/data/person_ids.txt";
	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */
	@Override
	public Class<RiskAssessment> getResourceType() {
		// TODO Auto-generated method stub
		return RiskAssessment.class;
	}
	@Read()
	public RiskAssessment getResourceById(@IdParam IdDt theId) {
		RiskAssessment risk = null;
		String resourceId = theId.getIdPart();
		System.out.println(resourceId);
		String[] Ids = theId.getIdPart().split("\\-", 2);
		System.out.println(Ids);
		
		return risk;
		
	}

	@Search()
	public List<RiskAssessment> getRiskAssessmentbySubject(
			@RequiredParam(name = Condition.SP_SUBJECT) ReferenceParam theSubject) {
		ArrayList<RiskAssessment> retV = null;
		return retV;
		
	}
	
	
	@Create()
	public MethodOutcome createRiskAssessment(@ResourceParam RiskAssessment theRisk){
		validateResource(theRisk);
		
		DataSource datasource = null;
		Connection connection = null;
		Statement statement = null;
		Context context = null;
		ArrayList<Integer> idList = new ArrayList<Integer>();

		int size = theRisk.getBasis().size();
		String PatientId = null;
		int count = 0;
		/*while(count !=size){
			PatientId = theRisk.getBasis().get(count).getReference().getIdPart();
			//System.out.println(theRisk.getBasis().get(count).getReference().getIdPart());
			count = count+1;
		}*/
		//System.out.println(size);
		//System.out.println(theRisk.getBasis().get(0).getReference().getIdPart());
		
		//int tempNum = 4;
		

		BufferedWriter out = null;
		try  
		{
		    FileWriter fstream = new FileWriter(patientFile, false); //true tells to append data.
		    out = new BufferedWriter(fstream);
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
		
		Process p = null;
		int ret = 0;
		StringBuilder sb = new StringBuilder();
		try {
			String pbCommand[] = { "python", predictModel};
			//ProcessBuilder pb = new ProcessBuilder(pbCommand);
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
		//System.out.println(sb);
		String finalString = sb.toString();
		
		JSONObject obj;
		JSONObject objPatient = null;
		try {
			obj = new JSONObject(finalString);
			JSONArray arr = obj.getJSONArray("prediction");
			for (int i = 0; i < arr.length(); i++){
				objPatient = new JSONObject(arr.getString(i));
				String personId = objPatient.getString("person_id");
				Double score = objPatient.getDouble("score");
				
				context = new InitialContext();
				datasource = (DataSource) context.lookup("java:/comp/env/jdbc/HealthPort");
				connection = datasource.getConnection();
				statement = connection.createStatement();
				String SQL_Count = "SELECT COUNT(*) FROM RISKASSESSMENT WHERE PATIENTID='"+ personId +"' AND SCORE='"+score+"'";
				ResultSet check_ret = statement.executeQuery(SQL_Count);
				check_ret.next();
				
				String SQL_Statement = null;
				if (check_ret.getInt(1) == 0) {
					SQL_Statement = "INSERT INTO RISKASSESSMENT (PATIENTID, SCORE) VALUES ('"+personId+ "', '"+score+"')";
					statement.executeUpdate(SQL_Statement);
				} 
			
				//statement.executeUpdate(SQL_Statement);
				
				SQL_Statement = "SELECT ID FROM RISKASSESSMENT WHERE PATIENTID='"+ personId +"' AND SCORE='"+score+"'";
				check_ret = statement.executeQuery(SQL_Statement);
				check_ret.next();
				//System.out.println(check_ret.getInt(1));
				idList.add(check_ret.getInt(1));
				
				connection.close();
			    //System.out.println(personId);
				//System.out.println(score);
			}

		} catch (JSONException | NamingException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String finalId = idList.get(0).toString();
		int sizeId = idList.size();
		for (int j =1; j <sizeId;j++ ){
			finalId = finalId + "-"+idList.get(j).toString();
		}
		
		//System.out.println(finalId);
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
}
