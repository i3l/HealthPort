package edu.gatech.i3l.HealthPort;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import edu.gatech.i3l.HealthPort.ConditionResourceProvider;
import edu.gatech.i3l.HealthPort.ObservationResourceProvider;
import edu.gatech.i3l.HealthPort.PatientResourceProvider;

/**
 * In this example, we are using Servlet 3.0 annotations to define the URL
 * pattern for this servlet, but we could also define this in a web.xml file.
 */
@WebServlet(urlPatterns = { "/fhir/*" }, displayName = "FHIR Server")
public class RestfulServlet extends RestfulServer {

	private static final long serialVersionUID = 1L;

	/**
	 * The initialize method is automatically called when the servlet is
	 * starting up, so it can be used to configure the servlet to define
	 * resource providers, or set up configuration, interceptors, etc.
	 */
	@Override
	protected void initialize() throws ServletException {
		/*
		 * The servlet defines any number of resource providers, and configures
		 * itself to use them by calling setResourceProviders()
		 */
		List<IResourceProvider> resourceProviders = new ArrayList<IResourceProvider>();
		resourceProviders.add(new PatientResourceProvider());
		resourceProviders.add(new ObservationResourceProvider());
		resourceProviders.add(new ConditionResourceProvider());
		resourceProviders.add(new MedicationPrescrResource());
		setResourceProviders(resourceProviders);

//		/*
//		 * Use a narrative generator. This is a completely optional step, but
//		 * can be useful as it causes HAPI to generate narratives for resources
//		 * which don't otherwise have one.
//		 */
//		INarrativeGenerator narrativeGen = new DefaultThymeleafNarrativeGenerator();
//		getFhirContext().setNarrativeGenerator(narrativeGen);
//
//		/*
//		 * Tells HAPI to use content types which are not technically FHIR
//		 * compliant when a browser is detected as the requesting client. This
//		 * prevents browsers from trying to download resource responses instead
//		 * of displaying them inline which can be handy for troubleshooting.
//		 */
//		setUseBrowserFriendlyContentTypes(true);
	}

	/**
	 * Constructor
	 */
	// public ExRestfulServlet() {
	// /*
	// * The servlet defines any number of resource providers, and
	// * configures itself to use them by calling
	// * setResourceProviders()
	// */
	// //System.out.println("HERE");
	// List<IResourceProvider> resourceProviders = new
	// ArrayList<IResourceProvider>();
	// resourceProviders.add(new PatientResourceProvider());
	// resourceProviders.add(new ObservationResourceProvider());
	// resourceProviders.add(new ConditionResourceProvider());
	// resourceProviders.add(new MedicationPrescrResource());
	// setResourceProviders(resourceProviders);
	// }
}