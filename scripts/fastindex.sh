#!/usr/bin/sh -v
#Use this script to index FAST data files
#Requires the epadd (standalone) jar and a settings file that specifies the locations of the FAST data files
#Please read doc/README before using this script
 
if [ "$#" -ne 2 ] || ! [ -e "$1" ] || ! [ -e "$2" ]; then
    echo "Error!! Less/more than 2 arguements provided or one or more of provided arguements do not exist">&2;
    echo "#########################">&2;
    echo "Usage: script <absolue path to standalone jar> <absolute path settings file (json)>">&2;
    echo "#########################">&2;
    exit 1;
fi

pwd=$PWD;
mkdir /tmp/epadd;
cd /tmp/epadd;
echo "Standalone jar: "$1;
echo "Settings file: "$2;
jar xvf $1 epadd.war;
jar xvf epadd.war;

LIB=WEB-INF/lib/;
CP="";
for lib in $LIB/*.jar;
do
    CP=$CP:$lib;
done
java -Xmx1g -cp $CP FASTIndexer $2
rm -R /tmp/epadd/