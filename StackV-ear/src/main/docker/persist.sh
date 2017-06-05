#!/bin/bash

###############
## Admin script (/bin/persist-rainsdb.sh) to restart wildfly with persistent database.
###############

set -m
set -e

# modify ./StackV-ejb/src/main/resources/META-INF/persistence.xml to replace 'drop-and-create' with 'none'
jar xf /opt/jboss/wildfly/standalone/deployments/StackV-ear-1.0-SNAPSHOT.ear StackV-ejb-1.0-SNAPSHOT.jar
jar xf StackV-ejb-1.0-SNAPSHOT.jar META-INF/persistence.xml
sed -i "s/drop-and-create/none/g" META-INF/persistence.xml
jar uf StackV-ejb-1.0-SNAPSHOT.jar META-INF/persistence.xml
jar uf /opt/jboss/wildfly/standalone/deployments/StackV-ear-1.0-SNAPSHOT.ear StackV-ejb-1.0-SNAPSHOT.jar
rm -rf StackV-ejb-1.0-SNAPSHOT.jar META-INF

