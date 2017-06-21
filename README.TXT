This text describes how to check if system parts are on-line.

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