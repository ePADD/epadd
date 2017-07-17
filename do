#!/bin/csh -fe
ant
# cd ../muse
# mvn clean
mvn clean
mvn -f pom-common.xml
# mvn # this step is needed
# cd ../epadd
mvn -f pom-discovery.xml
mvn 
