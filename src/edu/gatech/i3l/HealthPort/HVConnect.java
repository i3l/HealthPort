package edu.gatech.i3l.HealthPort;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;

import com.microsoft.hsg.ConnectionFactory;
import com.microsoft.hsg.HVAccessor;
import com.microsoft.hsg.Request;

/**
 * Servlet implementation class HelloWorld
 */
@WebServlet("/HVInfo")
public class HVConnect extends HttpServlet {
	private static final String USER_NAME = "name";
	private static final long serialVersionUID = 1L;
	public static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	public static final String JDBC_URL = "jdbc:derby:" + Config.DBPath+"/MyDB;user=adh;password=123";
	private String Record_id;
	private String Person_id;
	private Document document;
	//public static final String SQL_STATEMENT = "select * from cyborg.footable";
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HVConnect () {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//response.getWriter().write("hello");
		Connection connection = null;
		Statement statement = null;
		response.setContentType("text/html");
		PrintWriter printWriter = response.getWriter();
		printWriter.println("<html>");
		printWriter.println("<head> <title> Person Info </title></head>");
		printWriter.println("<body><table width = 30%><tr>");
		
		String uname = request.getParameter(USER_NAME);
		
		try{
			Class.forName(DRIVER);
			connection = DriverManager.getConnection(JDBC_URL);
			statement = connection.createStatement();
			
			String SQL_Statement1 = "select recordID from cdcAppDB.HVUSERS where name = "+ uname;
			String SQL_Statement2 = "select personId from cdcAppDB.HVUSERS where name = " + uname; 
			
			ResultSet resultSet = statement.executeQuery(SQL_Statement1);
			if (resultSet.next()) {
				Record_id  = resultSet.getString("recordID");
			}
			resultSet = statement.executeQuery(SQL_Statement2);
			if (resultSet.next()) {
				Person_id  = resultSet.getString("personID");
			}

		
		} catch (ClassNotFoundException e){
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			try{
				if(statement != null) statement.close();
				if(connection != null) connection.close();
			}catch (SQLException e){e.printStackTrace();}
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
        
		while((i=is.read())!=-1)
        {
           // converts integer to character
           c=(char)i;
           
           // prints character
           resString.append(c);
           //System.out.print(c);
        }
		
		//Remove the HV response tags
		String finalString;
		int intIndex = resString.indexOf("<Clinical");
		finalString = resString.substring(intIndex);
		intIndex = finalString.indexOf("<common>");
		finalString = finalString.substring(0, intIndex);
		System.out.println(finalString);
		
		String contextPath = request.getSession().getServletContext().getRealPath("/");
		String xmlFilePath=contextPath+"/patientCCD.xml";
		System.out.println(xmlFilePath);
		File myfile = new File(xmlFilePath);

		myfile.createNewFile(); 
		
		FileWriter fout = new FileWriter(myfile);
		fout.write(finalString);
		fout.close();
		
	
		
		//String xmlFormat = prettyFormat(resString.toString(),2);	
		//printWriter.println(xmlFormat);
		printWriter.println("</table></body></html>");
	}
	/*
	public static String prettyFormat(String input, int indent) {
	    try {
	        Source xmlInput = new StreamSource(new StringReader(input));
	        StringWriter stringWriter = new StringWriter();
	        StreamResult xmlOutput = new StreamResult(stringWriter);
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        transformerFactory.setAttribute("indent-number", indent);
	        Transformer transformer = transformerFactory.newTransformer(); 
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.transform(xmlInput, xmlOutput);
	        return xmlOutput.getWriter().toString();
	    } catch (Exception e) {
	        throw new RuntimeException(e); // simple exception handling, please review it
	    }
	}*/

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
