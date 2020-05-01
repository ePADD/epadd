REM call ant
REM cd ..\muse
call mvn clean
call mvn -f pom-common.xml
REM call mvn -f pom-jar.xmlm
REM call mvn 
REM cd ..\epadd
REM call mvn clean
call mvn -f pom-discovery.xml
call mvn 
