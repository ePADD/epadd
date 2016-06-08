ant
cd ..\muse
mvn -f pom-common.xml
mvn -f pom-jar.xml
mvn 
cd ..\epadd
mvn clean
mvn -f pom-discovery.xml
mvn 
