package edu.gatech.i3l.HealthPort.HV;

import com.microsoft.hsg.ApplicationConfig;

public class Config {

	//static String DBPath = "/opt/fhirDB";
	static String DBPath = "/Users/MC142/Documents/workspace/Servers/Databases";
	
	public static final String RedirectUrl = 
		ApplicationConfig.getValue("target.auth.redirect");
	public static final String ActionQS = 
		ApplicationConfig.getValue("target.auth.actionqs");
	public static final String ShellUrl = 
		ApplicationConfig.getValue("shell.url");
		
}
