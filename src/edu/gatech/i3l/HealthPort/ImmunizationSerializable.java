package edu.gatech.i3l.HealthPort;

import java.io.Serializable;

/**
 * Bean for Immunization data.
 * @author ewall
 */
public class ImmunizationSerializable implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String ID;
	public String NAMEURI;
	public String NAMECODING;
	public String NAMEDISPLAY;
	public String SUBJECT;
	java.util.Date VACCINATION_DATE;
	public String SERIES;
	public String MANUFACTURER;
	public String LOT_NUMBER;
	public String DOSE_QUANTITY;
	public String DOSE_UNITS;
	public String SITE;
	public String ROUTE;
	public String PERFORMER_ID;
	public String PERFORMER_NAME;
	public String ENCOUNTER_ID;
}
