/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.io.Serializable;

/**
 * @author MC142
 *
 */
public class ConditionSerializable implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String ID;
	public String NAMEURI;
	public String NAMECODING;
	public String NAMEDISPLAY;
	public String SUBJECT;
	public String STATUS;
	public java.util.Date ONSET;
	public java.util.Date DATEASSERTED;
	public String TEXTSTATUS;
	public String NARRATIVE;
}
