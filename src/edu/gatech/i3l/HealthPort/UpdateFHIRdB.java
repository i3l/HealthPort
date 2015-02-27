package edu.gatech.i3l.HealthPort;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.gatech.i3l.HealthPort.ports.ExactDataPort;
import edu.gatech.i3l.HealthPort.ports.GreenwayPort;
import edu.gatech.i3l.HealthPort.ports.HealthVaultPort;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Servlet implementation class UpdateFHIRdB
 */
@WebServlet(description = "Synchorize FHIR database with Ports", urlPatterns = { "/UpdateFHIRdB" }, initParams = { @WebInitParam(name = "r", value = "observation", description = "Update Observations") })
public class UpdateFHIRdB extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private ExactDataPort syntheticEHRPort;
	private HealthVaultPort healthvaultPort;
	private GreenwayPort greenwayPort;
	private HealthPortInfo healthPortInfo;
	private ExactDataPort syntheticCancerPort;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public UpdateFHIRdB() {
		super();

		syntheticEHRPort = new ExactDataPort("jdbc/ExactDataSample",
				ExactDataPort.SyntheticEHR);
		healthvaultPort = new HealthVaultPort();
		greenwayPort = new GreenwayPort();
		healthPortInfo = new HealthPortInfo("jdbc/HealthPort");
		syntheticCancerPort = new ExactDataPort("jdbc/ExactDataCancer",
				ExactDataPort.SyntheticCancer);

	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String res = request.getParameter("r");
		Connection connection = null;
		Statement statement = null;

		System.out.println("Update FHIR DB Request: " + res);

		String ccd = null;
		try {
			connection = healthPortInfo.getConnection();
			statement = connection.createStatement();
			String sql = "SELECT U1.ID, U1.ORGANIZATIONID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID)"
					+ "WHERE ORG.TAG='"
					+ GreenwayPort.GREENWAY
					+ "' OR ORG.TAG='" + HealthVaultPort.HEALTHVAULT + "'";
			ResultSet resultSet = statement.executeQuery(sql);

			while (resultSet.next()) {
				healthPortInfo.setRSInformation(resultSet);
				if (healthPortInfo.orgID.equals(greenwayPort.getId())) {
					ccd = GreenwayPort.getCCD(healthPortInfo.personId);
				} else if (healthPortInfo.orgID.equals(healthvaultPort.getId())) {
					healthvaultPort.getAllObservations(healthPortInfo);

				}
			}

			syntheticEHRPort.getObservations();
			syntheticCancerPort.getObservations();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
