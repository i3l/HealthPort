/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.io.Serializable;

/**
 * @author MC142
 *
 */
public class MedicationPrescriptionSerializable implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String ID;
	public String NAMEURI;
	public String NAMECODING;
	public String NAMEDISPLAY;
	public String SUBJECT;
	public String PRESCRIBER;
	public String DOSEQTY;
	public String DOSEUNIT;
	public String DOSAGEINSTRUCTION;
	public int DISPENSEQTY;
	public int REFILL;
	public java.util.Date DATEWRITTEN;
	public String STATUS;
	public String TEXTSTATUS;
	public String NARRATIVE;
}
