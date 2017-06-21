#!/bin/bash

cd /home/gateway

java -DpropertiesPath=1.properties -jar router.jar &

java -DpropertiesPath=1.properties -jar entry_point_http.jar &

java -DpropertiesPath=1.properties -jar entry_point_ftp.jar &

java -DpropertiesPath=1.properties -jar entry_point_soap.jar &

mongod
