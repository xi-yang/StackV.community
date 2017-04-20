#!/usr/bin/env bash


# if ${KEYSTORE} exists, configure the keystore for https in standalone.xml
if [ ! -z "${KEYSTORE}" ]; then
  if [ -f ${KEYSTORE} ]; then
    sed -i "s/\/opt\/jboss\/keycloak.jks/${KEYSTORE}/g" standalone.xml
  else
    echo "Error: SSL Keystore file ${KEYSTORE} does not exist!"
    echo " Hint: Use `docker run -v /host/config/path:/container/config/path`. Make sure your keystore file is in /host/config/path/. )"
    exit 1
  fi
fi

# add an amdin user
if [ $ADMIN_USER ] && [ $ADMIN_PASSWORD ]; then
    /opt/jboss/keycloak/bin/add-user-keycloak.sh -u $ADMIN_USER -p $ADMIN_PASSWORD >/dev/null
fi

# start service
export LAUNCH_JBOSS_IN_BACKGROUND=true
export JBOSS_PIDFILE=/opt/jboss/keycloak.pid
/opt/jboss/keycloak/bin/standalone.sh -b 0.0.0.0  &

# maintain main process
/bin/bash -c "while true; do sleep 1; done"                    

