call ant
cd ..\muse
call mvn -f pom-common.xml
call mvn -f pom-jar.xml
call mvn 
cd ..\epadd
call mvn clean
call mvn -f pom-discovery.xml
call mvn 
