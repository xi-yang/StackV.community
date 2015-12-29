#!/bin/bash
curl -X POST -d @driver-onos.xml --header "Content-Type:application/xml" http://localhost:8080/VersaStack-web/restapi/driver
sleep 120
head -1 onos-workflow.xml > onos-workflow2.xml
INSTANCEID=`curl -X GET http://127.0.0.1:8080/VersaStack-web/restapi/service/instance`
echo "<uuid>$INSTANCEID</uuid>" >> onos-workflow2.xml
tail +3 onos-workflow.xml >> onos-workflow2.xml
curl -X POST -d @onos-workflow2.xml --header "Content-Type:application/xml" http://localhost:8080/VersaStack-web/restapi/service/$INSTANCEID
echo "curl -X PUT http://localhost:8080/VersaStack-web/restapi/service/$INSTANCEID/propagate"
#curl -X PUT http://localhost:8080/VersaStack-web/restapi/service/$INSTANCEID/propagate
#echo $INSTANCEID
#curl -X PUT http://localhost:8080/VersaStack-web/restapi/service/$INSTANCEID/revert
