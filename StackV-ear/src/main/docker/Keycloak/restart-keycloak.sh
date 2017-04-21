#!/bin/bash

###############
## Admin script (/bin/restart-keycloak.sh) to restart the service after manual config changes.
###############

set -m
set -e

# stop keycloak
PID_FILE=//opt/jboss/keycloak.pid
if [ -f ${PID_FILE} ]; then
  kill -9 $(cat ${PID_FILE})
  rm -rf ${PID_FILE}
fi

# Start keycloak
export LAUNCH_JBOSS_IN_BACKGROUND=true
export JBOSS_PIDFILE=/opt/jboss/keycloak.pid
/opt/jboss/keycloak/bin/standalone.sh -b 0.0.0.0  &
exit $?

