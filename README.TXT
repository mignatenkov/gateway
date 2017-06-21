# Gateway Project

Project consists of 5 parts, 4 of which are date entry points, and the last one - is core processor and output point.
All of the configuration option are present in gateway.properties.

Data routes:
1. [email|ftp|http|soap] -> router -> mongo
2. mongo -> router -> output

------------------------------------
How to check if system parts are on-line:

* Router
Call REST method '/api/v1/1c/test/version' on the supposed to be on-line host
    curl -i -X GET http://localhost:8080/api/v1/1c/test/version

* EntryPointSOAP
Call REST method '/test/version' on the supposed to be on-line host
    curl -i -X GET http://localhost:8082/test/version

* EntryPointHTTP
Call REST method '/version' on the supposed to be on-line host
    curl -i -X GET http://localhost:8081/version
Thou can also check, if heartbeat.entry_point_http.log is updating. Updating frequency is 1 sec.

* EntryPointFTP
Check, if heartbeat.entry_point_ftp.log is updating. Updating frequency is 1 sec.

* EntryPointEMAIL
Check, if heartbeat.entry_point_email.log is updating. Updating frequency is 1 sec.


Notice! If part was started with SSL support enabled (see .properties file), thou should use HTTPS for REST checks.