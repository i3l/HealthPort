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
public class ObservationSerializable implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public String ID;
	public String NAMEURI;
	public String NAMECODING;
	public String NAMEDISPLAY;
	public String QUANTITY;
	public String UNIT;
	public String COMMENT;
	public String SUBJECT;
	public String STATUS;
	public String RELIABILITY;
	public java.util.Date APPLIES;
	public java.util.Date ISSUED;
	public String TEXTSTATUS;
	public String NARRATIVE;
}
