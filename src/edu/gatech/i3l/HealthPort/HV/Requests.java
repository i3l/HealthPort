package edu.gatech.i3l.HealthPort.HV;

import java.io.IOException;
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

/**
 * Servlet implementation class HelloWorld
 */
@WebServlet("/GetUsers")
public class Requests extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	public static final String JDBC_URL = "jdbc:derby:" + Config.DBPath+"/MyDB;user=adh;password=123";
	public static final String SQL_STATEMENT = "select name from cdcAppDB.Users";
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Requests() {
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
		printWriter.println("<head> <title> Users Available</title></head>");
		printWriter.println("<body><h2> Users Available </h2><br/></body>");
		printWriter.println("<body><table width = 30%><tr>");
		try{
			Class.forName(DRIVER);
			connection = DriverManager.getConnection(JDBC_URL);
			statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			printWriter.println("</tr>");
			while (resultSet.next()) {
				printWriter.println("<tr>");
				String Name = resultSet.getString("name");
				printWriter.println("<td bgcolor =lightblue>" +resultSet.getString("name")+ "</td>");
				printWriter.println("<td bgcolor =lightgrey><a href=\"/CDCWebApp/HVInfo?name=\'"+Name+"\'\">Get Info</a></td>");
				//System.out.println(Name + "\n");
				printWriter.println("</tr>");
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
		
		printWriter.println("</table></body></html>");

		
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
