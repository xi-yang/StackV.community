#!/bin/bash

###############
## Admin script (/bin/persist-rainsdb.sh) to restart wildfly with persistent database.
###############

set -m
set -e

# stop wildfly
PID_FILE=//opt/jboss/wildfly.pid
if [ -f ${PID_FILE} ]; then
  kill -9 $(cat ${PID_FILE})
  rm -rf ${PID_FILE}
fi

# modify ./StackV-ejb/src/main/resources/META-INF/persistence.xml to replace 'drop-and-create' with 'none'
jar xf /opt/jboss/wildfly/standalone/deployments/StackV-ear-1.0-SNAPSHOT.ear StackV-ejb-1.0-SNAPSHOT.jar
jar xf StackV-ejb-1.0-SNAPSHOT.jar META-INF/persistence.xml
sed -i "s/drop-and-create/none/g" META-INF/persistence.xml
jar uf StackV-ejb-1.0-SNAPSHOT.jar META-INF/persistence.xml
jar uf /opt/jboss/wildfly/standalone/deployments/StackV-ear-1.0-SNAPSHOT.ear StackV-ejb-1.0-SNAPSHOT.jar
rm -rf StackV-ejb-1.0-SNAPSHOT.jar META-INF

# Start wildfly
export LAUNCH_JBOSS_IN_BACKGROUND=true
export JBOSS_PIDFILE=/opt/jboss/wildfly.pid
/opt/jboss/wildfly/bin/standalone.sh -c standalone-full.xml -b 0.0.0.0  2>&1 > /dev/null &
exit $?
#/bin/bash -c "while true; do sleep 1; done"

