/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.io.Serializable;
import java.sql.Date;

/**
 * @author MC142
 *
 */
public class ImmunizationSerializable implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public String id;
	public String VaccineType;
	public String Route;
	public String Site;
	public String Subject;
	public String Text;
	public String VaccinationProtocol;
	public String LotNumber;
	public Date VaccineDate;
	public double DoseQuantity;
}