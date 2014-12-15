package edu.gatech.i3l.HealthPort;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import edu.gatech.i3l.HealthPort.ConditionResourceProvider;
import edu.gatech.i3l.HealthPort.ObservationResourceProvider;
import edu.gatech.i3l.HealthPort.PatientResourceProvider;

/**
 * In this example, we are using Servlet 3.0 annotations to define
 * the URL pattern for this servlet, but we could also
 * define this in a web.xml file.
 */
@WebServlet(urlPatterns= {"/fhir/*"}, displayName="FHIR Server")
public class ExRestfulServlet extends RestfulServer {
 
    private static final long serialVersionUID = 1L;
 
    /**
     * Constructor
     */
    public ExRestfulServlet() {
        /*
         * The servlet defines any number of resource providers, and
         * configures itself to use them by calling
         * setResourceProviders()
         */
    	//System.out.println("HERE");
        List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();
        resourceProviders.add(new PatientResourceProvider());
        resourceProviders.add(new ObservationResourceProvider());
        resourceProviders.add(new ConditionResourceProvider());
        setResourceProviders(resourceProviders);
    }
     
}