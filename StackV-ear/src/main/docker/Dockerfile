
#################

### Deployment Instructions

### cd $STACKV_HOME
### mvn clean install -DskipTests  -Pdeploy-nuke
### docker build -f ./StackV-ear/src/main/docker/Dockerfile -t stackv .
### docker run -p8443:8443 -p 8080:8080 -d -t -e KEYCLOAK=k152.maxgigapop.net stackv

#################

# Use latest jboss/base-jdk:8 image as the base
FROM jboss/base-jdk:8

# Set the WILDFLY_VERSION env variable
ENV WILDFLY_VERSION 10.1.0.Final
ENV WILDFLY_SHA1 9ee3c0255e2e6007d502223916cefad2a1a5e333
ENV JBOSS_HOME /opt/jboss/wildfly

USER root

# Add the WildFly distribution to /opt, and make wildfly the owner of the extracted tar content
# Make sure the distribution is available from a well-known place
RUN cd $HOME \
    && curl -O https://download.jboss.org/wildfly/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.tar.gz \
    && sha1sum wildfly-$WILDFLY_VERSION.tar.gz | grep $WILDFLY_SHA1 \
    && tar xf wildfly-$WILDFLY_VERSION.tar.gz \
    && mv $HOME/wildfly-$WILDFLY_VERSION $JBOSS_HOME \
    && rm wildfly-$WILDFLY_VERSION.tar.gz \
    && chown -R jboss:jboss ${JBOSS_HOME} \
    && chmod -R g+rw ${JBOSS_HOME} \
    && sed -i "s/-Xms64m -Xmx512m/-Xms1024m -Xmx4096m/g" /opt/jboss/wildfly/bin/standalone.conf



ADD ./StackV-ear/src/main/docker/standalone-full.xml /opt/jboss/wildfly/standalone/configuration/standalone-full.xml

# Generate /opt/jboss/wildfly.jks for SSL and add keycloak-selfsigned.crt to server ssl ca-bundles
ADD ./StackV-ear/src/main/docker/keycloak-selfsigned.crt /opt/jboss/keycloak-selfsigned.crt

RUN \
    keytool -genkey -keyalg RSA -dname "CN=Wildfly Server,OU=Unit,O=Organization,L=City,S=State,C=US" -keypass changeit -keystore /opt/jboss/wildfly.jks -storepass changeit  \
    && cat /opt/jboss/keycloak-selfsigned.crt >> /etc/pki/tls/certs/ca-bundle.crt


## install mariadb
RUN yum install -y mariadb-server mariadb-client

## init mysql databases (frontend and rainsdb), add users / pass
ADD ./StackV-web/src/main/webapp/tools/localhost.sql /opt/jboss/localhost.sql

RUN \
  mysql_install_db --user=mysql --ldata=/var/lib/mysql/ 2>&1 > /dev/null  && \
  echo "/bin/mysqld_safe &" > /tmp/config && \
  echo "mysqladmin --silent --wait=30 ping || exit 1" >> /tmp/config && \
  echo "mysql -e 'CREATE USER \"login_view\"@\"localhost\" IDENTIFIED BY \"loginuser\";'" >> /tmp/config && \
  echo "mysql -e 'CREATE USER \"front_view\"@\"localhost\" IDENTIFIED BY \"frontuser\";'" >> /tmp/config && \
  echo "mysql -e 'GRANT ALL ON login.* TO \"login_view\"@\"localhost\";'" >> /tmp/config && \
  echo "mysql -e 'GRANT ALL ON frontend.* TO \"front_view\"@\"localhost\";'" >> /tmp/config && \
  echo "mysql -uroot -e 'CREATE DATABASE rainsdb;'" >> /tmp/config && \
  echo "mysql < /opt/jboss/localhost.sql" >> /tmp/config && \
  echo "mysql -e 'UPDATE mysql.user SET password=PASSWORD(\"root\") WHERE user=\"root\" and host=\"localhost\";'" >> /tmp/config && \
  echo "mysql -e 'GRANT ALL PRIVILEGES ON *.* TO \"root\"@\"localhost\" WITH GRANT OPTION;'" >> /tmp/config && \
  bash /tmp/config && \
  rm -f /tmp/config


## make jboss sudoer
RUN yum install -y sudo wget killall ntp
RUN echo "jboss ALL=(root) NOPASSWD: /bin/mysqld_safe, /bin/mysql_install_db, /sbin/ntpd" > /etc/sudoers.d/jboss \
    && chmod 0440 /etc/sudoers.d/jboss


# Entrypoint script to start mysqld and wildfly
ADD ./StackV-ear/src/main/docker/entrypoint.sh /bin/entrypoint.sh
RUN chown jboss:0 /bin/entrypoint.sh \
    && chmod 700 /bin/entrypoint.sh


# Admin scripts
ADD ./StackV-ear/src/main/docker/persist.sh /bin/persist.sh
ADD ./StackV-ear/src/main/docker/restart-persist.sh /bin/restart-persist.sh
RUN \
    chown jboss:0 /bin/persist.sh \
    && chmod 700 /bin/persist.sh \
    && chown jboss:0 /bin/restart-persist.sh \
    && chmod 700 /bin/restart-persist.sh

# Host command relay script
ADD ./StackV-ear/src/main/docker/hpc-relay.py /usr/bin/hpc-relay.py
RUN chmod 0755 /usr/bin/hpc-relay.py

# Ensure signals are forwarded to the JVM process correctly for graceful shutdown
ENV LAUNCH_JBOSS_IN_BACKGROUND true

USER jboss

## deploy StackV ear
ADD ./StackV-ear/target/StackV-ear-1.0-SNAPSHOT.ear /opt/jboss/wildfly/standalone/deployments/

# Install Keycloak adaptor
RUN cd ${JBOSS_HOME} \
    && wget https://downloads.jboss.org/keycloak/3.0.0.Final/adapters/keycloak-oidc/keycloak-wildfly-adapter-dist-3.0.0.Final.tar.gz \
    && tar zxf keycloak-wildfly-adapter-dist-3.0.0.Final.tar.gz \
    && ./bin/jboss-cli.sh --file=./bin/adapter-install-offline.cli \
    && rm -rf ./standalone/configuration/standalone_xml_history/current

# Install mysql driver to wildfly
RUN cd ${JBOSS_HOME} \
    && wget https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.41.tar.gz \
    && tar zxf mysql-connector-java-5.1.41.tar.gz \
    && mkdir -p ./modules/com/mysql/main \
    && mv mysql-connector-java-5.1.41/mysql-connector-java-5.1.41-bin.jar ./modules/com/mysql/main/ \
    && rm -rf mysql-connector-java-5.1.41* \
    && echo '<module xmlns="urn:jboss:module:1.3" name="com.mysql"> \
  <resources> \
    <resource-root path="mysql-connector-java-5.1.41-bin.jar"/> \
  </resources> \
  <dependencies> \
    <module name="javax.api"/> \
  </dependencies> \
</module>' >> ./modules/com/mysql/main/module.xml

# Expose the ports we're interested in
EXPOSE 8080 8443 9990 9993


ENTRYPOINT ["/bin/bash", "/bin/entrypoint.sh"]

