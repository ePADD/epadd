#!/bin/csh -fe
ant
cd ../muse
mvn -f pom-common.xml
mvn -f pom-jar.xml
mvn # this step is needed
cd ../epadd
mvn clean
mvn -f pom-discovery.xml
mvn 
