#!/usr/bin/env bash

jmc_version=`echo $(basename -a $JAVA_HOME/lib/missioncontrol/plugins/com.oracle.jmc.*.jar) | sed 's/.*_\(.*\)\.jar/\1/'`
mvn clean package -Djmc.version=$jmc_version