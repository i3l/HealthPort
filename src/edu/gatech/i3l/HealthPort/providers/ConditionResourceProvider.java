package edu.gatech.i3l.HealthPort.providers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import edu.gatech.i3l.HealthPort.HealthPortInfo;
import edu.gatech.i3l.HealthPort.ports.GreenwayPort;
import edu.gatech.i3l.HealthPort.ports.HealthVaultPort;
import edu.gatech.i3l.HealthPort.ports.ExactDataPort;

public class ConditionResourceProvider implements IResourceProvider {
	private ExactDataPort syntheticEHRPort;
	private HealthVaultPort healthvaultPort;
	private GreenwayPort greenwayPort;
	private HealthPortInfo healthPortUser;
	private ExactDataPort syntheticCancerPort;

	// Constructor
	public ConditionResourceProvider() {
		syntheticEHRPort = new ExactDataPort("jdbc/ExactDataSample",
				ExactDataPort.SyntheticEHR);
		healthvaultPort = new HealthVaultPort();
		greenwayPort = new GreenwayPort();
		healthPortUser = new HealthPortInfo("jdbc/HealthPort");
		syntheticCancerPort = new ExactDataPort("jdbc/ExactDataCancer",
				ExactDataPort.SyntheticCancer);
	}

	@Override
	public Class<? extends IResource> getResourceType() {
		// TODO Auto-generated method stub
		return Condition.class;
	}

	@Read()
	public Condition getResourceById(@IdParam IdDt theId) {
		Condition cond = null;
		String[] Ids = theId.getIdPart().split("\\.", 2);

		if (Ids[0].equals(healthvaultPort.getId())
				|| Ids[0].equals(greenwayPort.getId())) {

			healthPortUser.setInformation(Ids[1]);
			String orgID = healthPortUser.orgID;

			if (orgID.equals(greenwayPort.getId())) {
				System.out.println("Greenway");
			} else if (orgID.equals(healthvaultPort.getId())) {
				cond = healthvaultPort.getCondition(Ids[1]);
			}

		} else if (Ids[0].equals(syntheticEHRPort.getId())) {
			cond = syntheticEHRPort.getCondition(Ids[1]);
		} else if (Ids[0].equals(syntheticCancerPort.getId())) {
			cond = syntheticCancerPort.getCondition(Ids[1]);
		}

		return cond;
	}

	@Search
	public List<Condition> getAllConditions() {
		Connection connection = null;
		Statement statement = null;
		String ccd = null;

		ArrayList<Condition> finalRetVal = new ArrayList<Condition>();
		ArrayList<Condition> retVal = null;

		try {
			connection = healthPortUser.getConnection();
			statement = connection.createStatement();
			String sql = "SELECT U1.ID, U1.NAME, ORG.TAG, U1.RECORDID, U1.PERSONID, U1.GENDER, U1.CONTACT, U1.ADDRESS FROM USER AS U1 LEFT JOIN ORGANIZATION AS ORG ON (ORG.ID=U1.ORGANIZATIONID)"+
					"WHERE ORG.TAG='"+GreenwayPort.GREENWAY+"' OR ORG.TAG='"+HealthVaultPort.HEALTHVAULT+"'";

			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				healthPortUser.setRSInformation(resultSet);

				if (healthPortUser.orgID.equals(greenwayPort.getId())) {
					ccd = GreenwayPort.getCCD(healthPortUser.personId);
					// System.out.println(ccd);
				} else if (healthPortUser.orgID
						.equals(healthvaultPort.getId())) {
					retVal = healthvaultPort.getConditions(healthPortUser);
					if (retVal != null && !retVal.isEmpty()) {
						finalRetVal.addAll(retVal);
					}
				}
				
				retVal = null;
			}
			connection.close();

			retVal = syntheticEHRPort.getConditions();
			if (retVal != null && !retVal.isEmpty()) {
				finalRetVal.addAll(retVal);
			}

			retVal = syntheticCancerPort.getConditions();
			if (retVal != null && !retVal.isEmpty()) {
				finalRetVal.addAll(retVal);
			}

			retVal = null;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return finalRetVal;
	}

	@Search()
	public List<Condition> getConditionsByPatient(
			@RequiredParam(name = Condition.SP_SUBJECT) ReferenceParam theSubject) {

		String orgID = null;
		String ccd = null;

		ArrayList<Condition> retVal = null;

		String Ids[] = theSubject.getIdPart().split("\\.", 2);

		if (Ids[0].equals(healthvaultPort.getId()) || Ids[0].equals(greenwayPort.getId())) {
			healthPortUser.setInformation(Ids[1]);
			String pId = healthPortUser.personId;
			orgID = healthPortUser.orgID;
			if (orgID != null) {
				if (orgID.equals(greenwayPort.getId())) {
					ccd = GreenwayPort.getCCD(pId);
					// System.out.println(ccd);
				} else if (orgID.equals(healthvaultPort.getId())) {
					// retVal = new
					// HealthVaultPort().getConditions(HealthPortUser);
					retVal = healthvaultPort.getConditions(healthPortUser);
				}
			}
		} else {
			if (Ids[0].equals(syntheticEHRPort.getId())) {
				// retVal = new
				// SyntheticEHRPort().getConditions(HealthPortUser);
				retVal = syntheticEHRPort.getConditionsByPatient(Ids[1]);
			} else if (Ids[0].equals(syntheticCancerPort.getId())) {
				// retVal = new
				// SyntheticEHRPort().getConditions(HealthPortUser);
				retVal = syntheticCancerPort.getConditionsByPatient(Ids[1]);
			}
		}

		return retVal;
	}

	@Search()
	public List<Condition> searchByCode(
			@RequiredParam(name = Condition.SP_CODE) TokenParam theId) {
		String codeSystem = theId.getSystem();
		String code = theId.getValue();
		// System.out.println(codeSystem);
		// System.out.println(code);

		ArrayList<Condition> retVal = new ArrayList<Condition>();
		ArrayList<Condition> portRet = null;
		// retVal = new SyntheticEHRPort().getConditionsByCodeSystem(codeSystem,
		// code);
		// return retVal;
		portRet = syntheticEHRPort.getConditionsByCodeSystem(codeSystem, code);
		if (portRet != null && !portRet.isEmpty()) {
			retVal.addAll(portRet);
		}

		portRet = syntheticCancerPort.getConditionsByCodeSystem(codeSystem,
				code);
		if (portRet != null && !portRet.isEmpty()) {
			retVal.addAll(portRet);
		}

		return retVal;
	}

}