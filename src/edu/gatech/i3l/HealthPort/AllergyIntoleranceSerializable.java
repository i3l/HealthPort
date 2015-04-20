package edu.gatech.i3l.HealthPort;

import java.io.Serializable;

/**
 * Bean for AllergyIntolerance data.
 * @author ewall
 */
public class AllergyIntoleranceSerializable implements Serializable {
	private static final long serialVersionUID = 1L;

    public String ID;
    public String SUBJECT;
    public String SUBST_DISPLAY;
    public String SUBST_CODE;
    public String SUBST_SYSTEM;
    public String SENSITIVITY_TYPE;
    public java.util.Date RECORDED_DATE;
    public String REACTION;
    public String CRITICALITY;
    public String STATUS;
}