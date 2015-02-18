/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.util.ArrayList;

import ca.uhn.fhir.model.dev.resource.Condition;
import ca.uhn.fhir.model.dev.resource.MedicationPrescription;
import ca.uhn.fhir.model.dev.resource.Observation;

/**
 * @author MC142
 *
 */
public interface HealthPortFHIRIntf {
	// Observation Resource 
	public ArrayList<Observation> getObservations (HealthPortUserInfo userInfo);
	public Observation getObservation(String resourceId);
	public ArrayList<Observation> getObservationsByCodeSystem (String codeSystem, String code);
	
	// Condition Resource
	public ArrayList<Condition> getConditions (HealthPortUserInfo userInfo);
	public Condition getCondition(String resourceId);
	public ArrayList<Condition> getConditionsByCodeSystem (String codeSystem, String code);
	
	// MedicationPrescription Resource
	public ArrayList<MedicationPrescription> getMedicationPrescriptions (HealthPortUserInfo userInfo);
	public MedicationPrescription getMedicationPrescription(String resourceId);
}
