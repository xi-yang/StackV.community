#!/bin/bash

###############
#?# TODO: Add admin script (/bin/persist-rainsdb.sh) to stop wildfly, modify ./StackV-ejb/src/main/resources/META-INF/persistence.xml 
#?# 	  to replace 'drop-and-create' with 'none' and then restart wildfly
###############

set -m
set -e

KEYCLOAK=${KEYCLOAK:-k152.maxgigapop.net}

# Reconfigure keycloak server url in standalone-full.xml and keycloak.json
sed -i "s/k152.maxgigapop.net/${KEYCLOAK}/g" /opt/jboss/wildfly/standalone/configuration/standalone-full.xml

# Reconfigure keycloak server in keycloak.json files for StackV-ear-1.0-SNAPSHOT.ear
jar xf /opt/jboss/wildfly/standalone/deployments/StackV-ear-1.0-SNAPSHOT.ear StackV-web-1.0-SNAPSHOT.war
jar xf StackV-web-1.0-SNAPSHOT.war WEB-INF/keycloak.json
jar xf StackV-web-1.0-SNAPSHOT.war data/json/keycloak.json
sed -i "s/k152.maxgigapop.net/${KEYCLOAK}/g" WEB-INF/keycloak.json
jar uf StackV-web-1.0-SNAPSHOT.war WEB-INF/keycloak.json
sed -i "s/k152.maxgigapop.net/${KEYCLOAK}/g" data/json/keycloak.json
jar uf StackV-web-1.0-SNAPSHOT.war data/json/keycloak.json
jar uf /opt/jboss/wildfly/standalone/deployments/StackV-ear-1.0-SNAPSHOT.ear StackV-web-1.0-SNAPSHOT.war 
rm -rf StackV-web-1.0-SNAPSHOT.war WEB-INF data

# if ${KEYSTORE} exists, configure the keystore for https in standalone-full.xml
if [ ! -z "${KEYSTORE}" ]; then 
  if [ -f ${KEYSTORE} ]; then
    sed -i "s/\/opt\/jboss\/wildfly.jks/${KEYSTORE}/g" /opt/jboss/wildfly/standalone/configuration/standalone-full.xml
  else
    echo "Error: SSL Keystore file ${KEYSTORE} does not exist!"
    echo " Hint: Use `docker run -v /host/config/path:/container/config/path`. Make sure your keystore file is in /host/config/path/. )"
    exit 1
  fi
fi

# if ${TRUSTCERT} exists, append the file to /etc/pki/tls/certs/ca-bundle.crt
if [ ! -z "${TRUSTCERT}" ]; then 
  if [ -f ${TRUSTCERT} ]; then
    cat ${TRUSTCERT} >> /etc/pki/tls/certs/ca-bundle.crt
  else
    echo "Error: CA Cert file ${TRUSTCERT} does not exist!"
    echo "( Hint: Use `docker run -v /host/config/path:/container/config/path`. Make sure your trusted cert file is in /host/config/path/. )"
    exit 1
  fi
fi

if [ $ADMIN_USER ] && [ $ADMIN_PASSWORD ]; then
    /opt/jboss/wildfly/bin/add-user.sh -u $ADMIN_USER -p $ADMIN_PASSWORD >/dev/null
fi

# Start ntpd
/bin/sudo /sbin/ntpd &

# Start mysqld
/bin/sudo /bin/mysqld_safe &

# Start wildfly
export LAUNCH_JBOSS_IN_BACKGROUND=true
export JBOSS_PIDFILE=/opt/jboss/wildfly.pid
/opt/jboss/wildfly/bin/standalone.sh -c standalone-full.xml -b 0.0.0.0  &

# maintain main process
/bin/bash -c "while true; do sleep 1; done" 

