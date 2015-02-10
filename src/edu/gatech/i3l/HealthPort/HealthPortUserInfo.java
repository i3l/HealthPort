/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * @author Myung Choi
 * Constructor requires UserID.
 *  - Connects to SQL database to obtain patient related parameters.
 */
public class HealthPortUserInfo {
	public String userId = null;
	public String name = null;
	public String recordId = null;
	public String personId = null;
	public String dataSource = null;
	public String gender = null;
	public String contact = null;
	public String address = null;
	
	static String HEALTHVAULT = "HV";
	static String GREENWAY = "GW";
	static String SyntheticEHR = "SyntheticEHR";
	
	public HealthPortUserInfo (int userId) {
		setInformation (userId);
	}
	
	/**
	 * 
	 */
	public HealthPortUserInfo() {
		// TODO Auto-generated constructor stub
	}

	private void setInformation (int userId) {
    	Connection connection = null;
		Statement statement = null;
		Context context = null;
		DataSource datasource = null;

		this.userId = String.valueOf(userId);
		
		try{
			context = new InitialContext();
			datasource = (DataSource) context.lookup("java:/comp/env/jdbc/HealthPort");
			connection = datasource.getConnection();
			statement = connection.createStatement();
			String SQL_STATEMENT = "SELECT U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID) WHERE U1.ID="+userId;
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			
			if (resultSet.next()) {
				name = resultSet.getString("NAME");
				dataSource = resultSet.getString("TAG");
				recordId = resultSet.getString("RECORDID");
				personId = resultSet.getString("PERSONID");
				gender = resultSet.getString("GENDER");
				contact = resultSet.getString("CONTACT");
				address = resultSet.getString("ADDRESS");
				
//				System.out.println("[HealthPortUserInfo]"+userId);
//				System.out.println("[HealthPortUserInfo]"+name+":"+dataSource);
			}
			
			connection.close();

		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
}
