package edu.gatech.i3l.HealthPort;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet("/User")
public class UserGrantAuth extends HttpServlet 
{
	public UserGrantAuth() {
        super();
        // TODO Auto-generated constructor stub
    }
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//response.getWriter().write("hello");

		response.setContentType("text/html");
		PrintWriter printWriter = response.getWriter();
		printWriter.println("<html>");
		printWriter.println("<head> <title> AC App Authentication </title></head>");
		printWriter.println("<body><h2> Welcome to the Anticoagulant Advisor App Authentication </h2><br/>");
		printWriter.println("Click the link below to login to HealthVault and grant access to the AC Advisor App<br/>");
		printWriter.println("<a href=\"/HealthPort/Auth\">Authorize App</a></body>");
		printWriter.println("</html>");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}