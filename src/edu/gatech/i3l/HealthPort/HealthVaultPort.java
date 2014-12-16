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

	static public String getCCD (String Record_id, String Person_id) {		
		
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
