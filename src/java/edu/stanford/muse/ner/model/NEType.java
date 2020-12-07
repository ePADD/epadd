package edu.stanford.muse.ner.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by vihari on 23/09/16.
 */
public class NEType {
    private static final Logger log =  LogManager.getLogger(NEType.class);

    /**
     * DO NOT change the code (that is the number assigned to each type) for the types.
     * For compactness, the codes are stored, such as in index.
     * Changing the codes may give unexpected results.
     * If changed, then revert to the values in the commit of the previous release -- muse commit: 52aab037434ab6883759eaf108ca70fe0a259473
     * https://github.com/ePADD/muse/commits/master
     * */
    public enum     Type{
        PERSON(0, null, "Person"),
        PLACE(3, null, "Place"),
            BUILDING(2, PLACE, "Building"),RIVER(4, PLACE, "River"),ROAD(5, PLACE, "Road"),
            MOUNTAIN(9, PLACE, "Mountain"),AIRPORT(10, PLACE, "Airport"),ISLAND(17, PLACE, "Island"),
            MUSEUM(18, PLACE, "Museum"), BRIDGE(19, PLACE, "Bridge"), HOSPITAL(25, PLACE, "Hospital"),
            THEATRE(31, PLACE, "Theater"),LIBRARY(33, PLACE, "Library"),MONUMENT(35, PLACE, "Monument"),
        ORGANISATION(11, null, "Organisation"),
            COMPANY(1, ORGANISATION, "Company"),UNIVERSITY(7, ORGANISATION, "University"),
            PERIODICAL_LITERATURE(13, ORGANISATION, "Periodical Literature"),AIRLINE(20, ORGANISATION, "Airline"),
            GOVAGENCY(22, ORGANISATION, "Government Agency"),AWARD(27, ORGANISATION, "Award"),
            LEGISLATURE(32,ORGANISATION, "Legislature"),LAWFIRM(34, ORGANISATION, "Law firm"),
            DISEASE(36, ORGANISATION, "Disease"),EVENT(37, ORGANISATION, "Event"),
        //any other type that is not one of types above
        OTHER(38, null, "Other");
        private final short code;
        private final Type parent;
        private final String displayName; // end-user friendly name

        Type(int code, Type parent, String displayName) {
            this.code = (short)code;
            this.parent = parent;
            this.displayName = displayName;
        }

        public short getCode(){
            return code;
        }

        Type parent(){
            return parent;
        }

        public String getDisplayName(){
            return displayName;
        }
    }

    private static final Map<Type, String[]> dbpediaTypesMap = new LinkedHashMap<>();
    static{
        dbpediaTypesMap.put(Type.PERSON, new String[]{"Person", "Agent"});
        dbpediaTypesMap.put(Type.PLACE, new String[]{"Place", "Park|Place", "ProtectedArea|Place", "PowerStation|Infrastructure|ArchitecturalStructure|Place", "ShoppingMall|Building|ArchitecturalStructure|Place"});
        dbpediaTypesMap.put(Type.COMPANY, new String[]{"Company|Organisation", "Non-ProfitOrganisation|Organisation"});
        dbpediaTypesMap.put(Type.BUILDING, new String[]{"Building|ArchitecturalStructure|Place", "Hotel|Building|ArchitecturalStructure|Place"});
        dbpediaTypesMap.put(Type.RIVER, new String[]{"River|Stream|BodyOfWater|NaturalPlace|Place", "Canal|Stream|BodyOfWater|NaturalPlace|Place", "Stream|BodyOfWater|NaturalPlace|Place", "BodyOfWater|NaturalPlace|Place", "Lake|BodyOfWater|NaturalPlace|Place"});
        dbpediaTypesMap.put(Type.ROAD, new String[]{"Road|RouteOfTransportation|Infrastructure|ArchitecturalStructure|Place"});
        dbpediaTypesMap.put(Type.UNIVERSITY, new String[]{"University|EducationalInstitution|Organisation", "School|EducationalInstitution|Organisation", "College|EducationalInstitution|Organisation"});
        dbpediaTypesMap.put(Type.MOUNTAIN, new String[]{"Mountain|NaturalPlace|Place", "MountainRange|NaturalPlace|Place"});
        dbpediaTypesMap.put(Type.AIRPORT, new String[]{"Airport|Infrastructure|ArchitecturalStructure|Place"});
        dbpediaTypesMap.put(Type.ORGANISATION, new String[]{"Organisation", "PoliticalParty|Organisation", "TradeUnion|Organisation"});
        dbpediaTypesMap.put(Type.PERIODICAL_LITERATURE, new String[]{"Newspaper|PeriodicalLiterature|WrittenWork|Work", "AcademicJournal|PeriodicalLiterature|WrittenWork|Work", "Magazine|PeriodicalLiterature|WrittenWork|Work"});
        dbpediaTypesMap.put(Type.ISLAND, new String[]{"Island|PopulatedPlace|Place"});
        dbpediaTypesMap.put(Type.MUSEUM, new String[]{"Museum|Building|ArchitecturalStructure|Place"});
        dbpediaTypesMap.put(Type.BRIDGE, new String[]{"Bridge|RouteOfTransportation|Infrastructure|ArchitecturalStructure|Place"});
        dbpediaTypesMap.put(Type.AIRLINE, new String[]{"Airline|Company|Organisation"});
        dbpediaTypesMap.put(Type.GOVAGENCY, new String[]{"GovernmentAgency|Organisation"});
        dbpediaTypesMap.put(Type.HOSPITAL, new String[]{"Hospital|Building|ArchitecturalStructure|Place"});
        dbpediaTypesMap.put(Type.AWARD, new String[]{"Award"});
        dbpediaTypesMap.put(Type.THEATRE, new String[]{"Theatre|Venue|ArchitecturalStructure|Place"});
        dbpediaTypesMap.put(Type.LEGISLATURE, new String[]{"Legislature|Organisation"});
        dbpediaTypesMap.put(Type.LIBRARY, new String[]{"Library|Building|ArchitecturalStructure|Place"});
        dbpediaTypesMap.put(Type.LAWFIRM, new String[]{"LawFirm|Company|Organisation"});
        dbpediaTypesMap.put(Type.MONUMENT, new String[]{"Monument|Place"});
        dbpediaTypesMap.put(Type.DISEASE, new String[]{"Disease|Medicine"});
        dbpediaTypesMap.put(Type.EVENT, new String[]{"SocietalEvent|Event"});
    }

    public static Type[] getAllTypes(){
        return Type.values();
    }

    public static Short[] getAllTypeCodes() {
        return Stream.of(NEType.getAllTypes()).map(Type::getCode).toArray(Short[]::new);
    }

    //Never returns NULL, returns OTHER in every other case
    public static Type parseDBpediaType(String typeStr){
        Type type = Type.OTHER;
        if(typeStr == null)
            return type;

        //strip "|Agent" in the end
        if(typeStr.endsWith("|Agent"))
            typeStr = typeStr.substring(0, typeStr.length()-6);
        String[] fs = typeStr.split("\\|");
        //the loop codes the string type that may look like "University|EducationalInstitution|Organisation|Agent" into the most specific type by looking at the biggest to smallest prefix.
        outer:
        for(int ti=0;ti<fs.length;ti++) {
            StringBuilder sb = new StringBuilder();
            for(int tj=ti;tj<fs.length;tj++) {
                sb.append(fs[tj]);
                if(tj<fs.length-1)
                    sb.append("|");
            }
            String st = sb.toString();
            for (Type t : Type.values()) {
                String[] allowT = dbpediaTypesMap.get(t);
                if (allowT != null)
                    for (String at : allowT)
                        if (st.equals(at)) {
                            type = t;
                            break outer;
                        }
            }
        }
        return type;
    }

    public static Type getTypeForCode(short c){
        Type type = Stream.of(getAllTypes()).filter(t->t.getCode()==c).findAny().orElse(Type.OTHER);
        if(type.getCode()!=c)
            log.warn("Unknown code: "+c);
        return type;
    }

    /**Given a type described in text returns a coarse coding for the type
     * for example: "University" -> [ORGANIZATION]*/
    public static NEType.Type getCoarseType(NEType.Type type){
        if(type == null)
            return NEType.Type.OTHER;
        if(type.parent()==null)
            return type;
        else
            return type.parent();
    }

    public static NEType.Type getTypeForDisplayCode(String display){
        for(Type etype : NEType.dbpediaTypesMap.keySet()){
            if(etype.getDisplayName().toLowerCase().equals(display.toLowerCase()))
                return etype;
        }
        return null;
    }
    public static NEType.Type getCoarseType(Short ct){
        return getCoarseType(getTypeForCode(ct));
    }

    public static void main(String[] args) {
        Stream.of(Type.values()).forEach(System.out::println);
        System.out.println(Type.valueOf("LIBRARY"));
    }
}
