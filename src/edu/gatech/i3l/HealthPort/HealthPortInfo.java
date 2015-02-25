/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * @author Myung Choi Constructor requires UserID. - Connects to SQL database to
 *         obtain patient related parameters.
 */
public class HealthPortInfo {
	public String userId = null;
	public String name = null;
	public String recordId = null;
	public String personId = null;
	public String source = null;
	public String orgID = null;
	public String gender = null;
	public String contact = null;
	public String address = null;

	// Database (or Organization) Names
	// public static String HEALTHVAULT = "HV";
	// public static String GREENWAY = "GW";
	// public static String SyntheticEHR = "SyntheticEHR";
	// public static String SyntheticCancer = "SyntheticCancer";

	private DataSource dataSource;

	/**
	 * 
	 */
	public HealthPortInfo() {
		databaseSetup("jdbc/HealthPort");
	}

	public HealthPortInfo(String jndiName) {
		databaseSetup(jndiName);
	}

	public HealthPortInfo(String jndiName, String userId) {
		databaseSetup(jndiName);
		setInformation(userId);
	}

	public static String findIdFromTag(String tag) {
		// read organization id that this tag is assigned to.

		String retVal = null;
		Connection connection = null;
		Statement statement = null;
		try {
			DataSource tempds = (DataSource) new InitialContext()
					.lookup("java:/comp/env/jdbc/HealthPort");

			connection = tempds.getConnection();
			;
			statement = connection.createStatement();
			String SQL_STATEMENT = "SELECT * FROM ORGANIZATION WHERE TAG='"
					+ tag + "'";
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);
			if (resultSet.next()) {
				retVal = resultSet.getString("ID");
			}
			connection.close();
		} catch (NamingException | SQLException e) {
			e.printStackTrace();
		}
		return retVal;
	}

	private void databaseSetup(String jndiName) {
		try {
			dataSource = (DataSource) new InitialContext()
					.lookup("java:/comp/env/" + jndiName);
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Connection getConnection() {
		try {
			return dataSource.getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public void resetInformation() {
		userId = null;
		name = null;
		recordId = null;
		personId = null;
		source = null;
		gender = null;
		contact = null;
		address = null;
	}

	public void setRSInformation(ResultSet rs) throws SQLException {
		userId = rs.getString("ID");
		name = rs.getString("NAME");
		source = rs.getString("TAG");
		orgID = rs.getString("ORGANIZATIONID");
		recordId = rs.getString("RECORDID");
		personId = rs.getString("PERSONID");
		gender = rs.getString("GENDER");
		contact = rs.getString("CONTACT");
		address = rs.getString("ADDRESS");
	}

	public void setInformation(String userId) {
		Connection connection = null;
		Statement statement = null;
		// Context context = null;
		// DataSource datasource = null;

		this.userId = userId;

		try {
			connection = getConnection();
			statement = connection.createStatement();
			String SQL_STATEMENT = "SELECT U1.ID, U1.ORGANIZATIONID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID) WHERE U1.ID="
					+ userId;
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);

			if (resultSet.next()) {
				setRSInformation(resultSet);
				// System.out.println("[HealthPortUserInfo]"+userId);
				// System.out.println("[HealthPortUserInfo]"+name+":"+dataSource);
			}

			connection.close();

		} catch (Exception e) {
			e.printStackTrace();
			resetInformation();
		}
	}

	public void setInformationPersonID(String Id, String OrgID) {
		Connection connection = null;
		Statement statement = null;
		// Context context = null;
		// DataSource datasource = null;

		this.personId = Id;

		try {
			connection = getConnection();
			statement = connection.createStatement();
			String SQL_STATEMENT = "SELECT U1.ID, U1.ORGANIZATIONID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID) WHERE U1.PERSONID='"
					+ Id + "' AND U1.ORGANIZATIONID='" + OrgID + "'";
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);

			if (resultSet.next()) {
				setRSInformation(resultSet);
				// System.out.println("[HealthPortUserInfo]"+userId);
				// System.out.println("[HealthPortUserInfo]"+name+":"+dataSource);
			}

			connection.close();

		} catch (Exception e) {
			e.printStackTrace();
			resetInformation();
		}
	}

	public String getOrgID(String Tag) {
		String orgID = null;

		try {
			Connection connection = getConnection();
			Statement statement = connection.createStatement();
			String SQL_STATEMENT = "SELECT * FROM ORGANIZATION WHERE TAG='"
					+ Tag + "'";
			ResultSet resultSet = statement.executeQuery(SQL_STATEMENT);

			if (resultSet.next()) {
				orgID = resultSet.getString("ID");
				// System.out.println("[HealthPortUserInfo]"+userId);
				// System.out.println("[HealthPortUserInfo]"+name+":"+dataSource);
			}

			connection.close();

		} catch (Exception e) {
			e.printStackTrace();
			resetInformation();
		}

		return orgID;
	}
}
