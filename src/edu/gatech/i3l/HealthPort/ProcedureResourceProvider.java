package edu.gatech.i3l.HealthPort.providers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu.resource.Procedure;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import edu.gatech.i3l.HealthPort.HealthPortInfo;

public class ProcedureResourceProvider implements IResourceProvider {
	private HealthPortInfo healthPortUser;

	// Constructor
	public ProcedureResourceProvider() {
		healthPortUser = new HealthPortInfo("jdbc/HealthPort");
	}

	@Override
	public Class<? extends IResource> getResourceType() {
		return Procedure.class;
	}

	@Read()
	public Procedure getResourceById(@IdParam IdDt theId) {
		List<String>Ids = new ArrayList<String>();
		Ids.add(theId.getIdPart());
		
		List<IResource> resourceList = null;
		try {
			resourceList = healthPortUser.getResourceList(HealthPortInfo.PROCEDURE, Ids);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (resourceList == null || resourceList.isEmpty()) return null;
		return (Procedure) resourceList.get(0);		
	}

	@Search()
	public IBundleProvider getProceduresByPatient(
			@RequiredParam(name = Procedure.SP_SUBJECT) ReferenceParam theSubject) {

		final InstantDt searchTime = InstantDt.withCurrentTime();
		String patientID = theSubject.getIdPart();

		final List<String> matchingResourceIds = healthPortUser.getResourceIdsByPatient(HealthPortInfo.PROCEDURE, patientID);

		return new IBundleProvider() {

			@Override
			public int size() {
				return matchingResourceIds.size();
			}

			@Override
			public List<IResource> getResources(int theFromIndex, int theToIndex) {
				int end = Math.min(theToIndex, matchingResourceIds.size());
				// System.out.println("From:"+theFromIndex+" To:"+theToIndex+" Total:"+matchingResourceIds.size());
				List<String> idsToReturn = matchingResourceIds.subList(
						theFromIndex, end);
				
				List<IResource> retVal = null;
				try {
					retVal = healthPortUser.getResourceList(HealthPortInfo.PROCEDURE,
							idsToReturn);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return retVal;
			}

			@Override
			public InstantDt getPublished() {
				return searchTime;
			}
		};
	}
}