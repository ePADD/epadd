This package contains classes that perform Named Entity Recognition (NER) for ePADD. 
The NER system extracts candidate entities from free text based on trivial patterns such as consecutive initial capital words and are classified by the model into one of the multiple semantic types it can handle. <br>
The readily available NER model is trained on DBpedia instance file (of 2014 with 3.02M lines) and the CONLL lists for person, location, organization.
The NER model `SeqModel.ser.gz`, processed DBpedia instance file `instance_types_2014-04.en.txt.bz2` and CONLL `CONLL folder` can be found in the epadd-settings folder (https://github.com/ePADD/epadd-settings).<br>

##How to add more data to training
It is possible to include more lists to the training pipeline by appending the resource paths in epadd-settings/config.properties.
In the config.properties edit the following line with the right paths.
> NER_RESOURCE_FILE=CONLL/lists/ePADD.ned.list.LOC:::CONLL/lists/ePADD.ned.list.ORG:::CONLL/lists/ePADD.ned.list.PER

Note that the resources should be placed in the epadd-settings folder and observe that the file paths are delimited with `:::`.
All the resource files are expected in a format explained in `Training-data format` section.<br>
Once the properties file is edited as required, open your terminal `Applications>Utilities>Terminal.app` and run the following command
<br>
* if you have access to the *muse standalone jar*<br>
>java -Xmx3g -jar [ABSOLUTE PATH TO MUSE STANDALONE JAR] edu.stanford.muse.ner.model.SequenceModel  

* if you only have access to the *epadd standalone jar*<br>
>jar xvf [ABSOLUTE PATH TO EPADD STANDALONE JAR] epadd.war<br>
>jar xvf epadd.war WEB-INF/lib/muse-1.0.0-SNAPSHOT-classes.jar<br>
>java -Xmx3g -jar WEB-INF/lib/muse-1.0.0-SNAPSHOT-classes.jar edu.stanford.muse.ner.model.SequenceModel<br>

Once the training is launched, it will take 30-50 minutes depending on the hardware. The model will be written to `HOME/epadd-settings/SeqModel.ser.gz`, check the modified date of the model to make sure the same. 

##Training-data format
All the files included for training should adhere to the format described here. 
Each line should span information about an entry and should contain two fields separated by ` `[space].
The first field is the full name of the entry with spaces replaced by `_`, as in `New_York_Times`.
The second field is its type, we use DBPedia ontology (http://downloads.dbpedia.org/2015-04/dbpedia_2015-04.nt.bz2) 
to represent types in order to avoid any disambiguities and can help in clearly marking the super-class and sub-classes.  
For examplem the type of `New_York_Times` is `Newspaper|PeriodicalLiterature|WrittenWork|Work` which marks that `Newspaper` 
is a subclass of `PeriodicalLiterature` which is a subclass of `WrittenWork` and so on. Explicitly marking such information 
can allow us to group sparse types into a superclass, for example in the ePADD system, currently, we map each of `Newspaper|PeriodicalLiterature|WrittenWork|Work`,
`AcademicJournal|PeriodicalLiterature|WrittenWork|Work`, `Magazine|PeriodicalLiterature|WrittenWork|Work` to `PeriodicalLiterature|WrittenWork|Work`.<br>
For the purpose of illustration, a typical training data file looks as shown below

>Jack_Brittingham Person|Agent<br>
>Blotched_snake_eel Fish|Animal|Eukaryote|Species<br>
>Baptist_New_Meeting_House Building|ArchitecturalStructure|Place<br>
>KHOW RadioStation|Broadcaster|Organisation|Agent<br>
>Rahimabad-e_Sofla Village|Settlement|PopulatedPlace|Place<br>
>Udden_Gadda Settlement|PopulatedPlace|Place<br>
>Merupu_Daadi Film|Work<br>
>Soldatov's_gudgeon Fish|Animal|Eukaryote|Species<br>
>The_Wonderful_World_of_Stu TelevisionShow|Work<br>

##How to update the DBpedia resources
###Updating DBpedia instance file
DBpedia mines data from Wikipedia to extract structured content from it.
It assigns a type (with DBpedia ontology) to entities based on semi-structured content on Wikipedia such as Infobox and curated extraction framework.
The ePADD system uses this list of entries and its type for the training of NER model by default.  
DBpedia generally updates the resource twice in two years. 
The instance file released by DBpedia should be further processed in order to be used for training.
The resource file can be found at http://wiki.dbpedia.org, download the instance types file in **NT format** and follow the instructions below.<br>
If you do not have a *muse standalone jar* but only an *epadd standalone*, use the instructions provided in `How to train on more data` to extract muse jar from epadd jar.
Open the terminal in your system `Applications>Utilities>Terminal` and trigger the following commands
> java -Xmx3g -jar [ABSOLUTE PATH TO MUSE STANDALONE JAR] edu.stanford.muse.wpmine.DBpediaTypeParser [Full PATH to instance file you downloaded above] [Latest DBpedia ontology file] [full path of folder to output the post-processed file]

for example
> java -Xmx3g -jar [ABSOLUTE PATH TO MUSE STANDALONE JAR] edu.stanford.muse.wpmine.DBpediaTypeParser instance_types_en.nt.bz2 dbpedia_2015-04.nt.bz2 /Users/vihari/epadd-settings/

where instance_types_en.nt.bz2 and dbpedia_2015-04.nt.bz2 are resources downloaded off the DBpedia domain.<br>
The instance file downloaded from DBpedia would look like this
>\<http://dbpedia.org/resource/Autism> \<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> \<http://dbpedia.org/ontology/Disease> .<br>
>\<http://dbpedia.org/resource/Autism> \<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> \<http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#Situation> .<br>
>\<http://dbpedia.org/resource/Aristotle> \<http://www.w3.org/1999/02/22-rdf-syntax-ns#type> \<http://dbpedia.org/ontology/Philosopher> .

After processing, the generated file should like this
>Autism Disease|Medicine<br>
>Aristotle Philosopher|Person|Agent<br>
>Alabama AdministrativeRegion|Region|PopulatedPlace|Place<br>
>Abraham_Lincoln OfficeHolder|Person|Agent<br>

Make sure this is the case.

The post-processed file should be moved to your local `HOME/epadd-settings` folder and the `epadd-settings/config.properties` file should be modified to edit/include the following line
>DBPEDIA_INSTANCE_FILE=[The file name of the just processed instance file]