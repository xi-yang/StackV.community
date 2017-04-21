
#################

### Deployment Instructions 

### docker build -f Dockerfile -t keycloak .
### docker run -p8543:8543 -d -t -e KEYSTORE=/config/keycloak.jks keycloak

#################

# Use latest jboss/base-jdk:8 image as the base
FROM jboss/base-jdk:8

ENV KEYCLOAK_VERSION 3.0.0.Final
# Enables signals getting passed from startup script to JVM
# ensuring clean shutdown when container is stopped.
ENV LAUNCH_JBOSS_IN_BACKGROUND 1
ENV JBOSS_PIDFILE /opt/jboss/keycloak.pid

USER root

RUN yum install -y epel-release && yum install -y jq ntp && yum clean all

# Admin scripts
ADD ./restart-keycloak.sh /bin/restart-keycloak.sh
RUN \
    chown jboss:0 /bin/restart-keycloak.sh \
    && chmod 700 /bin/restart-keycloak.sh \
    && ln -s /opt/jboss/keycloak/bin/add-user-keycloak.sh /bin/add-user.sh \
    && systemctl enable ntpd

USER jboss

RUN cd /opt/jboss/ \
    && curl -L https://downloads.jboss.org/keycloak/$KEYCLOAK_VERSION/keycloak-$KEYCLOAK_VERSION.tar.gz | tar zx \
    && mv /opt/jboss/keycloak-$KEYCLOAK_VERSION /opt/jboss/keycloak \
    && sed -i "s/-Xms64m -Xmx512m/-Xms1024m -Xmx4096m/g" /opt/jboss/keycloak/bin/standalone.conf
    

ADD standalone.xml /opt/jboss/keycloak/standalone/configuration/standalone.xml

ADD keycloak-selfsigned.jks /opt/jboss/keycloak.jks

ADD StackV-realm.json /opt/jboss/StackV-realm.json

ADD entrypoint.sh /opt/jboss/

ENV JBOSS_HOME /opt/jboss/keycloak

EXPOSE 8543

ENTRYPOINT ["/bin/bash", "/opt/jboss/entrypoint.sh" ]

