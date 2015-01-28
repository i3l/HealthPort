/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.model.dstu.resource.Observation;

/**
 * @author MC142
 *
 */
public class SyntheticEHRPort implements HealthPortFHIRIntf {

	/* (non-Javadoc)
	 * @see edu.gatech.i3l.HealthPort.HealthPortFHIRIntf#getObservations(edu.gatech.i3l.HealthPort.HealthPortUserInfo)
	 */
	@Override
	public ArrayList<Observation> getObservations(HealthPortUserInfo userInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.gatech.i3l.HealthPort.HealthPortFHIRIntf#getConditions(edu.gatech.i3l.HealthPort.HealthPortUserInfo)
	 */
	@Override
	public ArrayList<Condition> getConditions(HealthPortUserInfo userInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.gatech.i3l.HealthPort.HealthPortFHIRIntf#getMedicationPrescriptions(edu.gatech.i3l.HealthPort.HealthPortUserInfo)
	 */
	@Override
	public ArrayList<MedicationPrescription> getMedicationPrescriptions(
			HealthPortUserInfo userInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.gatech.i3l.HealthPort.HealthPortFHIRIntf#getObservation(java.lang.String)
	 */
	@Override
	public Observation getObservation(String resourceId) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.gatech.i3l.HealthPort.HealthPortFHIRIntf#getCondition(java.lang.String)
	 */
	@Override
	public Condition getCondition(String resourceId) {
		// TODO Auto-generated method stub
		return null;
	}

}
