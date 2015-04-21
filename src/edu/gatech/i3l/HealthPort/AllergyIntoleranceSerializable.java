package edu.gatech.i3l.HealthPort;

import java.io.Serializable;

/**
 * Bean for AllergyIntolerance data.
 * @author Eric Wallace <ewall@gatech.edu>
 */
public class AllergyIntoleranceSerializable implements Serializable {
	private static final long serialVersionUID = 1L;

    public String ID;
    public String DISPLAY;
    public String SUBSTANCE;
    public String SUBJECT;
    public String SENSITIVITY_TYPE;
    public java.util.Date RECORDED_DATE;
    public String REACTION;
    public String CRITICALITY;
    public String STATUS;
    
    public AllergyIntoleranceSerializable() {};
    
    public AllergyIntoleranceSerializable(String id, String display, String substId, String subj,
    		String sensitivity, java.util.Date recDate, String reaction, String criticality, String status) {
    	this.ID = id;
    	this.DISPLAY = display;
    	this.SUBSTANCE = substId;
    	this.SUBJECT = subj;
    	this.SENSITIVITY_TYPE = sensitivity;
    	this.RECORDED_DATE = recDate;
    	this.REACTION = reaction;
    	this.CRITICALITY = criticality;
    	this.STATUS = status;
    }
}