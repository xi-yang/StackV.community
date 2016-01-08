#!/bin/bash
curl -X POST -d @driver-onos.xml --header "Content-Type:application/xml" http://localhost:8080/VersaStack-web/restapi/driver
sleep 120
VERSION=`curl -X GET -H "Accept: application/xml" http://127.0.0.1:8080/VersaStack-web/restapi/model|head -1|cut -d '>' -f8|cut -d'<' -f1`
head -3 onos-delta.xml > onos-delta2.xml
echo "<referenceVersion>$VERSION</referenceVersion>" >> onos-delta2.xml
tail +5 onos-delta.xml >> onos-delta2.xml
INSTANCEID=`curl -X GET http://127.0.0.1:8080/VersaStack-web/restapi/model/systeminstance`
curl -X POST -d @onos-delta2.xml --header "Content-Type:application/xml" http://localhost:8080/VersaStack-web/restapi/delta/$INSTANCEID/propagate
echo $INSTANCEID
#curl -X PUT http://localhost:8080/VersaStack-web/restapi/service/$INSTANCEID/revert
