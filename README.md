HealthPort
==========
HealthPort is a FHIR server that provides health data in the FHIR resource objects. HealthPort currently talks to 
HealthVault (PPE) and Greenway (API test) to retrieve health data. 

Supported FHIR Resources
<ul> 
<li> 
Patient: list available patients<br/>
(ex: http://taurus.i3l.gatech.edu:8080/CDCWebApp/fhir/Patient)
</li>
<li>
Observation by Patient: list of observations for a specific patient<br/>
(ex: http://taurus.i3l.gatech.edu:8080/CDCWebApp/fhir/Observation?subject:Patient=2)
</li>
<li>
Condition by Patient: list of conditions for a specific patient<br/>
(ex: http://taurus.i3l.gatech.edu:8080/CDCWebApp/fhir/Condition?subject:Patient=2)
</li>
</ul>

FHIR server is implemented using HAPI-FHIR Restful Server [http://jamesagnew.github.io/hapi-fhir/doc_rest_server.html]
