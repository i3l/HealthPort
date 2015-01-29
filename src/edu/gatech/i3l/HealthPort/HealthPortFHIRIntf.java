/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.util.ArrayList;

import ca.uhn.fhir.model.dstu.resource.Condition;
import ca.uhn.fhir.model.dstu.resource.MedicationPrescription;
import ca.uhn.fhir.model.dstu.resource.Observation;

/**
 * @author MC142
 *
 */
public interface HealthPortFHIRIntf {
	public ArrayList<Observation> getObservations (HealthPortUserInfo userInfo);
	public ArrayList<Condition> getConditions (HealthPortUserInfo userInfo);
	public ArrayList<MedicationPrescription> getMedicationPrescriptions (HealthPortUserInfo userInfo);
	public Observation getObservation(String resourceId);
	public Condition getCondition(String resourceId);
	public MedicationPrescription getMedicationPrescription(String resourceId);
}
