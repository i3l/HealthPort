/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.io.Serializable;

/**
 * @author MC142
 *
 */
public class ImmunizationSerializable implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String ID;
	public String SUBJECT;
	public String VACCINETYPE;
	public String ORGANIZATION;	
	public java.util.Date DATE;
}
