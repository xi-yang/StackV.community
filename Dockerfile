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
    && chown -R jboss:0 ${JBOSS_HOME} \
    && chmod -R g+rw ${JBOSS_HOME}


## customize standalone-full.xml (copy from template in StackV source, e.g. ejb max-threads = 100)
	# with https (fixed configs, with self-signed keystore that trusts self-signed keycloak server)
	# with datasources (fixed rainsdb and user/pass - MysqlDS + LoggerDS)
	# with keycloak (fixed configured with localhost for initial kc_url)

## install mariadb
RUN yum install -y mariadb-server mariadb-client

RUN systemctl enable mariadb

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

##?? install mysql driver to wildfly

##?? install keycloak adaptor to wildfly


## make jboss sudoer
RUN yum install -y sudo 
RUN echo "jboss ALL=(root) NOPASSWD: /bin/mysqld_safe" > /etc/sudoers.d/jboss && \
    chmod 0440 /etc/sudoers.d/jboss


# Ensure signals are forwarded to the JVM process correctly for graceful shutdown
ENV LAUNCH_JBOSS_IN_BACKGROUND true

USER jboss

## deploy StackV ear
ADD ./StackV-ear/target/StackV-ear-1.0-SNAPSHOT.ear /opt/jboss/wildfly/standalone/deployments/

## Run wildfly once to create rainsdb. Then stop it and modify persistence.xml for StackV ear 


# Expose the ports we're interested in
EXPOSE 8080 8443

# Set the default command to run on boot
# This will boot WildFly in the standalone mode and bind to all interface

## Replace CMD with ENTRYPOINT that takes -e keycloak_server to replace standalone-full.xml and ear/war keycloak.json 
	# optional: keystore at /keystore/wildfly.jks -> script to change keystore config in standalone-full.xml

ADD ./StackV-ear/src/main/docker/entrypoint.sh /opt/jboss/entrypoint.sh

ENTRYPOINT ["/bin/bash", "/opt/jboss/entrypoint.sh"]

#CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-c", "standalone-full.xml", "-b", "0.0.0.0"]
