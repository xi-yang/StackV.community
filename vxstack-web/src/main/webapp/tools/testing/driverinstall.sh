#!/bin/bash
curl -X POST -d @driver-ops.xml --header "Content-Type:application/xml" http://127.0.0.1:8080/VersaStack-web/restapi/driver;
curl -X POST -d @driver-vns.xml --header "Content-Type:application/xml" http://127.0.0.1:8080/VersaStack-web/restapi/driver;
curl -X POST -d @driver-stub-wan.xml --header "Content-Type:application/xml" http://127.0.0.1:8080/VersaStack-web/restapi/driver;
curl -X POST -d @driver-stub-fake.xml --header "Content-Type:application/xml" http://127.0.0.1:8080/VersaStack-web/restapi/driver;
curl -X POST -d @driver-stack-anl.xml --header "Content-Type:application/xml" http://127.0.0.1:8080/VersaStack-web/restapi/driver;