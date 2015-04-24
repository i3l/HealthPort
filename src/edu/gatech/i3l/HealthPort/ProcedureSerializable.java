/**
 * 
 */
package edu.gatech.i3l.HealthPort;

import java.io.Serializable;

/**
 * @author kfrazer3
 *
 */
public class ProcedureSerializable implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String ID;
	public String SUBJECT;
	public String TYPEURI;
	public String TYPECODING;
	public String TYPEDISPLAY;
	public String INDICATIONURI;
	public String INDICATIONCODING;
	public String INDICATIONTEXT;
	//public String PERFORMER;
	public String OUTCOME;
	public String REPORT;
	public String COMPLICATIONURI;
	public String COMPLICATIONCODING;
	public String COMPLICATIONTEXT;
	public String FOLLOWUP;
	public String NOTES;
	public java.util.Date STARTDATE;
	public java.util.Date ENDDATE;
}