#!/bin/bash

echo Testing DNC Creation API call.\n
curl -X POST -d @/Users/rikenavadur/NetBeansProjects/FrontendGENI/StackV/StackV-web/src/main/webapp/data/json/dnc_1.json --header "Content-Type:application/json" http://127.0.0.1:8080/StackV-web/restapi/app/service

echo Testing VNC Creation API call.\n
curl -X POST -d @/Users/rikenavadur/NetBeansProjects/FrontendGENI/StackV/StackV-web/src/main/webapp/data/json/netcreate_1.json --header "Content-Type:application/json" http://127.0.0.1:8080/StackV-web/restapi/app/service;