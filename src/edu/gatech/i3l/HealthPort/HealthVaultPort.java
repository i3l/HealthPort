package edu.gatech.i3l.HealthPort;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.microsoft.hsg.ConnectionFactory;
import com.microsoft.hsg.HVAccessor;
import com.microsoft.hsg.Request;

public class HealthVaultPort {
	public static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	public static final String JDBC_URL = "jdbc:derby:" + Config.DBPath+"/MyDB;user=adh;password=123";

	static public String getCCD (String uname) {
		String Record_id=null;
		String Person_id=null;
		Connection connection = null;
		Statement statement = null;
		
		try{
			Class.forName(DRIVER);
			connection = DriverManager.getConnection(JDBC_URL);
			statement = connection.createStatement();
			
			String SQL_Statement1 = "select recordID from cdcAppDB.HVUSERS where name = '"+ uname + "'";
			String SQL_Statement2 = "select personId from cdcAppDB.HVUSERS where name = '" + uname + "'"; 
			
			//String SQL_Statement1 = "select recordID from cdcAppDB.HVUSERS where name = "+ uname;
			//String SQL_Statement2 = "select personId from cdcAppDB.HVUSERS where name = " + uname; 
			
			ResultSet resultSet = statement.executeQuery(SQL_Statement1);
			if (resultSet.next()) {
				Record_id  = resultSet.getString("recordID");
			}
			resultSet = statement.executeQuery(SQL_Statement2);
			if (resultSet.next()) {
				Person_id  = resultSet.getString("personID");
			}
			
			connection.close();

		
		} catch (ClassNotFoundException e){
			e.printStackTrace();
			return null;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}finally{
			try{
				if(statement != null) statement.close();
				if(connection != null) connection.close();
			}catch (SQLException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		if (Person_id == null || Record_id == null) {
			return null;
		}
		
		StringBuilder requestXml = new StringBuilder();
		requestXml.append("<info><group>");
		requestXml.append("<filter><type-id>9c48a2b8-952c-4f5a-935d-f3292326bf54</type-id></filter>");
		requestXml.append("<format><section>core</section><section>otherdata</section><xml/></format>");
		requestXml.append("</group></info>");

		Request request2 = new Request();
		request2.setMethodName("GetThings");
		request2.setOfflineUserId(Person_id);
		request2.setRecordId(Record_id);
		request2.setInfo(requestXml.toString());

		HVAccessor accessor = new HVAccessor();
		accessor.send(request2, ConnectionFactory.getConnection());
		InputStream is = accessor.getResponse().getInputStream();
		
		int i;
		char c;
		
		StringBuilder resString = new StringBuilder();
        
		try {
			while((i=is.read())!=-1)
			{
			   // converts integer to character
			   c=(char)i;
			   
			   // prints character
			   resString.append(c);
			   //System.out.print(c);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return  null;
		}
		
		//Remove the HV response tags
		String finalString;
		int intIndex = resString.indexOf("<Clinical");
		finalString = resString.substring(intIndex);
		intIndex = finalString.indexOf("<common>");
		finalString = finalString.substring(0, intIndex);
		//System.out.println(finalString);
		
		return finalString;	
	}

}
