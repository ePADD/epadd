Building epadd:
There are 2 versions of ePADD; a normal one with the suffix epadd-) that is used for appraisal, processing and delivery modules, and another one (with the prefix epadd-discovery). ePADD is first built as a WAR file, and then packaged in the epadd-launcher project as a -standalone.jar, and then a .exe for Windows, and a .app and .dmg for Mac OS. 

You need to checkout 3 projects from git.

git clone https://github.com/ePADD/muse.git
git clone https://github.com/ePADD/epadd.git
git clone https://github.com/ePADD/epadd-launcher.git

To build epadd 
1. in the epadd folder, Type:
scripts/release.do

This will first build a muse.jar which is a dependency for ePADD. It then builds target/epadd-0.0.1-SNAPSHOT.war and target/epadd-discovery-0.0.1-SNAPSHOT.war

2. Next, in the epadd-launcher folder, Type: 
source do

(the above steps will work on a Linux/Mac OS X machine only -- we normally don't build on Windows, but the below scripts are probably easy to port):

When this operation is complete, there should be 6 files in the current folder:
epadd-standalone.jar
epadd.exe
epadd.dmg
and -discovery versions of these 3 files.

Explanation:

The muse project contains most of the Java based code. It is the core engine used in ePADD.
The epadd project contains primarily the frontend screens for the application (in the WebContent directory) and creates a deployable WAR file.
epadd-launcher bundles up the epadd.war into a standalone, runnable jar using an embedded Tomcat server.


