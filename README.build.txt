Building epadd:

There are 2 versions of ePADD; a full one with the suffix epadd-) that is used for appraisal, processing and delivery modules, and another one (with the prefix epadd-discovery). ePADD is first built as a WAR file, and then packaged in the epadd-launcher project as a -standalone.jar, and then a .exe for Windows, and a .app and .dmg for Mac OS. 

Make sure you have the Java 8 JDK installed, as well as the Java build tools maven and ant, and that mvn and ant are available in your path. Your JAVA_HOME should be pointing to the correct JDK installation directory. You can check this by ensuring that the following command gives the expected output.

$JAVA_HOME/bin/java -version (mac or linux)
$JAVA_HOME\bin\java -version (windows)

First checkout 3 projects from git.

git clone https://github.com/ePADD/muse.git
git clone https://github.com/ePADD/epadd.git
git clone https://github.com/ePADD/epadd-launcher.git
git clone https://github.com/ePADD/epadd-settings.git (this MUST be checked out under your home directory, so that it exists under ~/epadd-settings)

To build epadd 
1. in the epadd folder, Type:
./do (or do.bat on windows)

This will first build a muse.jar which is a dependency for ePADD. It then builds target/epadd-0.0.1-SNAPSHOT.war and target/epadd-discovery-0.0.1-SNAPSHOT.war

2. Next, in the epadd-launcher folder, Type: 
./do (do.bat on windows)

When this operation is complete, there should be 6 files in the current folder:
epadd-standalone.jar
epadd.exe
epadd.dmg
and -discovery versions of these 3 files.

Notes:

The muse project contains most of the Java based code. It is the core engine used in ePADD.  The epadd project contains primarily the frontend screens for the application (in the WebContent directory) and creates a deployable WAR file.  epadd-launcher bundles up the epadd.war into a standalone, runnable jar using an embedded Tomcat server.

3. To check whether the program built correctly, you can try to run:
java -Xmx2g -jar epadd-standalone.jar

4. To run automated test cases, first quit all running instances of ePADD. Then go to epadd-launcher, edit config.properties with the appropriate paths for your system and type mvn -f pom-test.xml test
The script in epadd-launcher/src/test/resources/feature/epadd.feature will be run.

5. To run the epadd-launcher in a debugger, add the following files to a temp directory and then add that directory to the classpath:
crossdomain.xml	
epadd.war	
index.html	
muse-icon.png

Running TomcatMain as the main class with the VM argument -splash:lib/splash-image.png will give exactly the same behaviour as running epadd-standalone.jar

