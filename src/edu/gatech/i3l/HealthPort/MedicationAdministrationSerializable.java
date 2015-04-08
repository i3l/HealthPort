/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.io.Serializable;

/**
 * @author Samuel_Tjokrosoesilo
 *
 */
public class MedicationAdministrationSerializable implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String ID;
	public String STATUS;
	public String SUBJECT;
	public String MEDICATION;
	public String DEVICEID;
	public String DEVICEDISPLAY;
	public int QTYVALUE;
	public String QTYUNIT;
}
