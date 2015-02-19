HealthPort
==========
HealthPort is a FHIR server that provides health data in the FHIR resource objects. HealthPort currently talks to 
HealthVault (PPE) and Greenway (API test) to retrieve health data. 

The server is only accessible from Georgia Tech campus network.

Supported FHIR Resources
<ul> 
<li> 
Patient: list available patients<br/>
(ex: https://taurus.i3l.gatech.edu:8443/CDCWebApp/fhir/Patient)
</li>
<li>
Observation by Patient: list of observations for a specific patient<br/>
(ex: https://taurus.i3l.gatech.edu:8443/CDCWebApp/fhir/Observation?subject:Patient=22)
</li>
<li>
Condition by Patient: list of conditions for a specific patient<br/>
(ex: https://taurus.i3l.gatech.edu:8443/CDCWebApp/fhir/Condition?subject=22)
</li>
</ul>

FHIR server is implemented using HAPI-FHIR Restful Server [http://jamesagnew.github.io/hapi-fhir/doc_rest_server.html]
