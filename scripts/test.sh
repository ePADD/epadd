#!/usr/bin/bash

LIB=target/epadd-0.0.1-SNAPSHOT/WEB-INF/lib/
#java -Xmx1g -cp target/bespoke-1.0.0-SNAPSHOT.jar:target/bespoke-1.0.0-SNAPSHOT/WEB-INF/lib/opennlp-tools-1.5.3.jar:target/bespoke-1.0.0-SNAPSHOT/WEB-INF/lib/gson-2.2.4.jar:target/bespoke-1.0.0-SNAPSHOT/WEB-INF/lib/kstem-3.4.jar:target/muse.jar:$LIB/muse-jar-1.0.0-SNAPSHOT.jar:$LIB/commons-httpclient-3.1.jar:$LIB/twitter4j-core-3.0.5.jar:$LIB/commons-logging-1.1.1.jar:$LIB/commons-codec-1.5.jar edu.stanford.bespoke.test.KEI
CP="";
for lib in $LIB/*.jar;
do
    CP=$CP:$lib;
done

#java -Xmx2g -cp $CP:target/classes edu.stanford.epadd.index.LocSubjectsIndexer test/sample4.txt #$HOME/sandbox/locsubjects/subjects-madsrdf-20140306.nt
java -Xmx1g -cp $CP:target/classes edu.stanford.epadd.index.FASTIndexer test/config.json
#java -Xmx1g -cp $CP:target/classes edu.stanford.epadd.index.TopicsSearcher test/sampleFAST.txt
#java -Xmx1g -cp $CP:target/classes edu.stanford.epadd.FASTReader /Users/viharipiratla/epadd-db/FASTPersonal.nt
#java -cp $CP:target/classes edu.stanford.epadd.EntityFeature "Maine"
#java -cp $CP:target/classes FreebaseSearcher "Slate magazine"