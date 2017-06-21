#!/bin/bash

openssl pkcs12 -export -inkey $1 -in $2 -passin pass:changeit -out keystore.p12 -passout pass:changeit -name $3 -password pass:changeit
keytool -importkeystore -destkeystore keystore.jks -deststorepass changeit -srckeystore keystore.p12 -srcstoretype PKCS12 -srcstorepass changeit