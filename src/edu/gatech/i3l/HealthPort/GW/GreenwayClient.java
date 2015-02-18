package edu.gatech.i3l.HealthPort.GW;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.gatech.i3l.HealthPort.ports.GreenwayPort;

/**
 * Servlet implementation class GreenwayClient
 */
@WebServlet("/GreenwayClient")
public class GreenwayClient extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GreenwayClient() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		String PatientID = request.getParameter("pid");
		if(PatientID == null || PatientID.isEmpty()){
			return;
		}
		String my_xml = GreenwayPort.getCCD(PatientID);
		
		response.setContentType("application/json");
		PrintWriter printWriter = response.getWriter();
		
		printWriter.println(my_xml);
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
