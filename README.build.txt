Building epadd:
There are 2 versions of ePADD; a normal one with the suffix epadd-) that is used for appraisal, processing and delivery modules, and another one (with the prefix epadd-discovery). ePADD is first built as a WAR file, and then packaged in the epadd-launcher project as a -standalone.jar, and then a .exe for Windows, and a .app and .dmg for Mac OS. 

To build epadd:
in the epadd folder:
scripts/release.do
This will build target/epadd-0.0.1-SNAPSHOT.war and target/epadd-discovery-0.0.1-SNAPSHOT.war

Next, in the epadd-launcher folder:
Type: source do

When this operation is complete, there should be 6 files in the current folder:
epadd-standalone.jar
epadd.exe
epadd.dmg
and -discovery versions of these 3 files.

