package edu.gatech.i3l.HealthPort;

import java.io.Serializable;

/**
 * Bean for Substance data.
 * @author Eric Wallace <ewall@gatech.edu>
 */
public class SubstanceSerializable implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String ID;
	public String NAME;
	public String TYPE_SYSTEM;
	public String TYPE_CODE;
	public String TYPE_DISPLAY;
	public String EXTENSION_SYSTEM;
	public String EXTENSION_CODE;
	public String EXTENSION_DISPLAY;
}