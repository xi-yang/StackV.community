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
jar xf StackV-web-1.0-SNAPSHOT.war resources/keycloak.json
sed -i "s/k152.maxgigapop.net/${KEYCLOAK}/g" WEB-INF/keycloak.json
jar uf StackV-web-1.0-SNAPSHOT.war WEB-INF/keycloak.json
sed -i "s/k152.maxgigapop.net/${KEYCLOAK}/g" resources/keycloak.json
jar uf StackV-web-1.0-SNAPSHOT.war resources/keycloak.json
jar uf /opt/jboss/wildfly/standalone/deployments/StackV-ear-1.0-SNAPSHOT.ear StackV-web-1.0-SNAPSHOT.war
rm -rf StackV-web-1.0-SNAPSHOT.war WEB-INF resources

# if ${KEYSTORE} exists, configure the keystore for https in standalone-full.xml
if [ ! -z "${KEYSTORE}" ]; then
  if [ -f ${KEYSTORE} ]; then
    sed -i "s/\/opt\/jboss\/wildfly.jks/${KEYSTORE//\//\\/}/g" /opt/jboss/wildfly/standalone/configuration/standalone-full.xml
  else
    echo "Error: SSL Keystore file ${KEYSTORE} does not exist!"
    echo " Hint: Use 'docker run -v /host/config/path:/container/config/path'. Make sure your keystore file is in /host/config/path/. )"
    exit 1
  fi
fi

# if ${TRUSTCERT} exists, append the file to /etc/pki/tls/certs/ca-bundle.crt
if [ ! -z "${TRUSTCERT}" ]; then
  if [ -f ${TRUSTCERT} ]; then
    cat ${TRUSTCERT} >> /etc/pki/tls/certs/ca-bundle.crt
  else
    echo "Error: CA Cert file ${TRUSTCERT} does not exist!"
    echo "( Hint: Use 'docker run -v /host/config/path:/container/config/path'. Make sure your trusted cert file is in /host/config/path/. )"
    exit 1
  fi
fi

if [ $ADMIN_USER ] && [ $ADMIN_PASSWORD ]; then
    /opt/jboss/wildfly/bin/add-user.sh -u $ADMIN_USER -p $ADMIN_PASSWORD >/dev/null
fi

# Start ntpd
/bin/sudo /sbin/ntpd &

# Stage and start mysqld
# if ${PERSISTED} and old DB  exists, call persist.sh
if [ ! -z "${PERSISTED}" ]; then
  if [ -f /var/lib/mysql/frontend/service.frm ]; then
    /bin/persist.sh
    /bin/sudo /bin/mysqld_safe &
  else
    ## If persisted StackV DB do not exist, re-prepare DBs and use the 'drop-and-create' option!
    sudo /bin/mysql_install_db --user=mysql --ldata=/var/lib/mysql/ 2>&1 > /dev/null
    echo "sudo /bin/mysqld_safe &" > /tmp/config && \
    echo "mysqladmin --silent --wait=30 ping || exit 1" >> /tmp/config && \
    echo "mysql -uroot -e 'CREATE USER \"login_view\"@\"localhost\" IDENTIFIED BY \"loginuser\";'" >> /tmp/config && \
    echo "mysql -uroot -e 'CREATE USER \"front_view\"@\"localhost\" IDENTIFIED BY \"frontuser\";'" >> /tmp/config && \
    echo "mysql -uroot -e 'GRANT ALL ON login.* TO \"login_view\"@\"localhost\";'" >> /tmp/config && \
    echo "mysql -uroot -e 'GRANT ALL ON frontend.* TO \"front_view\"@\"localhost\";'" >> /tmp/config && \
    echo "mysql -uroot -e 'CREATE DATABASE rainsdb;'" >> /tmp/config && \
    echo "mysql -uroot < /opt/jboss/localhost.sql" >> /tmp/config && \
    echo "mysql -uroot -e 'UPDATE mysql.user SET password=PASSWORD(\"root\") WHERE user=\"root\" and host=\"localhost\";'" >> /tmp/config && \
    echo "mysql -uroot -e 'GRANT ALL PRIVILEGES ON *.* TO \"root\"@\"localhost\" WITH GRANT OPTION;'" >> /tmp/config && \
    echo "mysql -uroot -e 'FLUSH PRIVILEGES;'" >> /tmp/config && \
    bash /tmp/config && \
    rm -f /tmp/config
  fi
else
  /bin/sudo /bin/mysqld_safe &
fi



# Start wildfly
export LAUNCH_JBOSS_IN_BACKGROUND=true
export JBOSS_PIDFILE=/opt/jboss/wildfly.pid
/opt/jboss/wildfly/bin/standalone.sh -c standalone-full.xml -b 0.0.0.0  &

# maintain main process
/bin/bash -c "while true; do sleep 1; done"

