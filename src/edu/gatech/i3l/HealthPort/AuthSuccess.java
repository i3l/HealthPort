package edu.gatech.i3l.HealthPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.microsoft.hsg.ConnectionFactory;
import com.microsoft.hsg.HVAccessor;
import com.microsoft.hsg.HVException;
import com.microsoft.hsg.Request;


@WebServlet("/AuthSuccess")
public class AuthSuccess extends HttpServlet 
{
	private static final String TOKEN_NAME = "wctoken";
	private static final long serialVersionUID = 1L;
	public static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	public static final String JDBC_URL = "jdbc:derby:" + Config.DBPath+"/MyDB;user=adh;password=123";
	//public static final String SQL_STATEMENT = "select * from cyborg.footable";
	
	public AuthSuccess() {
        super();
        // TODO Auto-generated constructor stub
    }
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		response.setContentType("text/html");
		PrintWriter printWriter = response.getWriter();
		printWriter.println("<html>");
		printWriter.println("<head> <title> Anticoagulant Advisor App Authentication </title></head>");
		
		String token = request.getParameter(TOKEN_NAME);
		PrintWriter pw = response.getWriter();
		if(token == null){
			printWriter.println("<body><h2> Authentication Failed </h2><br/></body>");
			printWriter.println("</html>");
		    System.out.println("It is null");
		    
		 } else {
			printWriter.println("<body><h2> Authentication Success </h2><br/></body>");
			printWriter.println("</html>");
		    pw.print("<html><body>" + token + "<body></html>");
		    getUserInfo(token);
		    
		    
		 }
	}
	
    private void getUserInfo(String userAuthToken)
	throws HVException
	{
    	try
    	{
    		Request request = new Request();
			request.setTtl(3600 * 8 + 300);
			request.setMethodName("GetPersonInfo");
			request.setUserAuthToken(userAuthToken);
			HVAccessor accessor = new HVAccessor();
			accessor.send(request, ConnectionFactory.getConnection());
			InputStream is = accessor.getResponse().getInputStream();
			XPath xpath = XPathFactory.newInstance().newXPath();
			
			Connection connection = null;
			Statement statement = null;
			
			Class.forName(DRIVER);
			connection = DriverManager.getConnection(JDBC_URL);
			statement = connection.createStatement();
			
			/*Node personInfo = (Node) xpath.evaluate("//person-info",
					new InputSource(is), XPathConstants.NODE);
	
			String pId = xpath.evaluate("person-id", personInfo);
			String pname = xpath.evaluate("name", personInfo);
			String rId = xpath.evaluate("record/@id", personInfo);
			
			
			
			String SQL_Statement1 = "INSERT INTO cdcAppDB.Users (name,location) VALUES (\'"+pname + "\','HV')";
			String SQL_Statement2 = "INSERT INTO cdcAppDB.HVUsers (name,recordID,personID) VALUES (\'"+pname +"\',\'"+ rId+"\',\'"+pId+"\')";
			
			
			statement.executeUpdate(SQL_Statement1);
			statement.executeUpdate(SQL_Statement2);*/
			
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
			Document someXML = documentBuilder.parse(is);
			Node personInfo = (Node) xpath.evaluate("//person-info", someXML, XPathConstants.NODE);


			String pId = xpath.evaluate("person-id", personInfo);
			String pname = xpath.evaluate("name", personInfo);
			String rId = xpath.evaluate("record/@id", personInfo);

			System.out.println ("pId:"+pId+", rId:"+rId+", name:"+pname);

			String SQL_Count = "SELECT COUNT(*) FROM cdcAppDB.Users";
			ResultSet check_ret = statement.executeQuery(SQL_Count);
			check_ret.next();
			Integer counter = check_ret.getInt(1);
			counter = counter +1;
			
			String SQL_CHECK = "SELECT COUNT(*) FROM cdcAppDB.Users WHERE name='"+pname+"'";
			check_ret = statement.executeQuery(SQL_CHECK);
			check_ret.next();
			String SQL_Statement1;
			String SQL_Statement2;
			if (check_ret.getInt(1) == 0) {
			SQL_Statement1 = "INSERT INTO cdcAppDB.Users (name,location,id) VALUES (\'"+pname + "\','HV',"+counter+")";
			SQL_Statement2 = "INSERT INTO cdcAppDB.HVUsers (name,recordID,personID) VALUES (\'"+pname +"\',\'"+ rId+"\',\'"+pId+"\')";
			} else {
			SQL_Statement1 = "UPDATE cdcAppDB.Users SET location = 'HV' WHERE name = '"+pname+"'";
			SQL_Statement2 = "UPDATE cdcAppDB.HVUsers set recordID='"+rId+"', personID='"+pId+"' WHERE name = '"+pname+"'";
			}

			statement.executeUpdate(SQL_Statement1);
			statement.executeUpdate(SQL_Statement2);
			
			
			
			
			connection.close();
    		
    		
    	}
    	catch(HVException he)
    	{
    		throw he;
    	}
    	catch(Exception e)
    	{
    		throw new HVException(e);
    	}
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}