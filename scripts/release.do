#!/bin/csh -fe
# set build_info = "Built by `whoami`, `date '+%a %d %b %Y %l:%M %p'`"
# java -version
# echo $build_info
# set version = `cat version`
# echo 'ePADD version '${version}  
# echo 'ePADD version '${version}  >! WebContent/version.jsp
# echo 'package edu.stanford.epadd;' >! src/java/edu/stanford/epadd/Version.java
# echo 'public class Version {  public static final String version = "'${version}'"; ' >> src/java/edu/stanford/epadd/Version.java
# echo 'public static final String buildInfo = "'${build_info}'";} ' >> src/java/edu/stanford/epadd/Version.java

ant
cd ../muse
mvn -f pom-common.xml
mvn -f pom-jar.xml
cd ../epadd
mvn clean
mvn -f pom-discovery.xml
mvn 
