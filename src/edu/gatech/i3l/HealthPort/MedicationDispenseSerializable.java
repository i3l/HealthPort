/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.io.Serializable;

/**
 * @author Samuel_Tjokrosoesilo
 *
 */
public class MedicationDispenseSerializable implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String ID;
	public String SUMMARY;
	public String STATUS;
	public int QTYVALUE;
	public String QTYUNIT;
	public String QTYCODE;
	public java.util.Date DATEPREPARED;
	public java.util.Date DATEHANDED;
	public String SUBSTCODE;
	public String SUBSTDISPLAY;
}
