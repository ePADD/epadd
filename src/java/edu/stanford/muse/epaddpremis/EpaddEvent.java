/*
    2022-11-03  added EpaddEvent(EventType eventType, String eventDetailInformation, String outcome, String mode, ZonedDateTime date1)
                added more EventType

*/
package edu.stanford.muse.epaddpremis;

import edu.stanford.epadd.Version;
import org.json.JSONObject;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class EpaddEvent implements Serializable {

    @XmlElement
    private EventIdentifier eventIdentifier;

    @XmlElement(name="eventType")
    @XmlJavaTypeAdapter(EventTypeAdapter.class)
    private EventType eventType;

    @XmlElement(name="eventDateTime")
    private String eventDateTime;

    @XmlElement(name="eventDetailInformation")
    private EventDetailInformation eventDetailInformation;

    @XmlElement
    private EventOutcomeInformation eventOutcomeInformation;

    @XmlElement(name="linkingAgentIdentifier")
    private LinkingAgentIdentifier linkingAgentIdentifier;

    @XmlElement
    private LinkingObjectIdentifier linkingObjectIdentifier;

    //Becomes part of linkingAgentIdentifierValue, so no need to keep it as XmlElement
    @XmlTransient
    private String mode;

    public EpaddEvent(EventType eventType, String eventDetailInformation, String outcome, String mode)
    {
        eventOutcomeInformation = new EventOutcomeInformation(outcome);
        linkingAgentIdentifier = new LinkingAgentIdentifier(mode);
        this.eventType = eventType;
        this.eventDetailInformation = new EventDetailInformation(eventDetailInformation);
        setValues();
    }
// 2022-11-03
    public EpaddEvent(EventType eventType, String eventDetailInformation, String outcome, String mode, ZonedDateTime date1)
    {
        eventOutcomeInformation = new EventOutcomeInformation(outcome);
        linkingAgentIdentifier = new LinkingAgentIdentifier(mode);
        this.eventType = eventType;
        this.eventDetailInformation = new EventDetailInformation(eventDetailInformation);
//        setValues();
        generateAndSetUuid();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        eventDateTime = date1.format(formatter);
    }

    public EpaddEvent() {
    }

    public EpaddEvent(EventType eventType, String eventDetailInformation, String outcome, String linkingObjectIdentifierType, String linkingObjectIdentifierValue, String mode)
    {
        this(eventType, eventDetailInformation, outcome, mode);
        linkingObjectIdentifier = new LinkingObjectIdentifier(linkingObjectIdentifierType, linkingObjectIdentifierValue);
    }

    public EpaddEvent(JSONObject eventJsonObject, String mode) {
        eventOutcomeInformation = new EventOutcomeInformation(eventJsonObject.getString("outcome"));
        linkingAgentIdentifier = new LinkingAgentIdentifier(mode);
        String eventString = eventJsonObject.getString("eventType");
        EpaddEvent.EventType eventType;
        if (eventString == null || eventString.isEmpty()) {
            eventType = EpaddEvent.EventType.NOT_RECOGNIZED;
        } else {
            eventType = EpaddEvent.EventType.fromString(eventString);
        }
        this.eventType = eventType;
        this.eventDetailInformation = new EventDetailInformation(eventJsonObject.getString("eventDetailInformation"));
        setValues();
    }

    private void setValues() {
        generateAndSetUuid();
        setEventDateTimeToNow();
    }

    private void generateAndSetUuid() {
        eventIdentifier = new EventIdentifier(UUID.randomUUID().toString());
    }

    private void setEventDateTimeToNow()
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        eventDateTime = ZonedDateTime.now().format(formatter);
    }

    public enum EventType {
// 2022-09-01 Added IMAP_INGEST		
// 2022-11-03	Added more EventType		
//        TRANSFER_TO_PROCESSING("transfer to processing"), TRANSFER_TO_DISCOVERY_AND_DELIVERY("transfer to discovery and delivery"), MBOX_INGEST("mbox ingest"), MBOX_EXPORT("mbox export"), EXPORT_FOR_PRESERVATION("export for preservation"), NOT_RECOGNIZED("not recognized");
        TRANSFER_TO_PROCESSING("transfer to processing"), TRANSFER_TO_DISCOVERY_AND_DELIVERY("transfer to discovery and delivery"), MBOX_INGEST("mbox ingest"), MBOX_EXPORT("mbox export"), EXPORT_FOR_PRESERVATION("export for preservation"), NOT_RECOGNIZED("not recognized"), 
        IMAP_INGEST("imap ingest"),
		
        INGESTION("Ingestion"),
        IDENTIFIER_ASSIGNMENT("Identifier assignment"),
        FIXITY_CHECK("Fixity check"),
        MESSAGE_DIGEST_CALCULATION("Message digest calculation"),
        QUARANTINE("Quarantine"),
        UNQUARANTINE("Unquarantine"),
        UNPACKING("Unpacking"),
        NAME_CLEANUP("Name cleanup"),
        VIRUS_CHECK("Virus check"),
        FORMAT_IDENTIFICATION("Format identification"),
        VALIDATION("Validation"),
        NORMALIZATION("Normalization"),
        TRANSCRIPTION("Transcription"),
        CREATION("Creation"),
        OTHER("Other")
        ;

        private final String eventType;

        EventType(String eventType) {
            this.eventType = eventType;
        }

        public static EventType fromString(String text) {
            for (EventType e : EventType.values()) {
                if (e.eventType.equalsIgnoreCase(text)) {
                    return e;
                }
            }
            return NOT_RECOGNIZED;
        }

        public String toString()
        {
            return eventType;
        }
    }

    public static class EventTypeAdapter extends XmlAdapter<String, EventType> implements Serializable
    {
        public String marshal(EventType eventType) {
            return eventType.toString();
        }
        public EventType unmarshal(String val) {
            return EventType.fromString(val);
        }
    }

    private static class EventIdentifier implements Serializable
    {
        @XmlElement
        private final String eventIdentifierType = "UUID";
        @XmlElement
        private String eventIdentifierValue;

        public EventIdentifier(){}

        public EventIdentifier(String eventIdentifierValue)
        {
            this.eventIdentifierValue = eventIdentifierValue;
        }
    }

    public static class EventDetailInformation implements Serializable
    {
        @XmlElement
        private String eventDetail;

        private EventDetailInformation(){}

        private EventDetailInformation(String eventDetail)
        {
            this.eventDetail = eventDetail;
        }
    }

    private static class EventOutcomeInformation implements Serializable {

        @XmlElement
        private String eventOutcome;

        private EventOutcomeInformation()
        {
        }

        private EventOutcomeInformation(String value) {
            eventOutcome = value;
        }
    }

    private static class LinkingObjectIdentifier implements Serializable
    {
        @XmlElement
        private String linkingObjectIdentifierType;
        @XmlElement
        private String linkingObjectIdentifierValue;
        private LinkingObjectIdentifier(){}

        private LinkingObjectIdentifier(String type, String value)
        {
            linkingObjectIdentifierType = type;
            linkingObjectIdentifierValue = value;
        }
    }

    private static class LinkingAgentIdentifier implements Serializable {

        @XmlElement
        private final String linkingAgentIdentifierType = "local";

        @XmlElement
        private String linkingAgentIdentifierValue;

        @XmlElement
        private final LinkingAgentRole linkingAgentRole = new LinkingAgentRole();

        private LinkingAgentIdentifier()
        {

        }

        private LinkingAgentIdentifier(String mode)
        {
            linkingAgentIdentifierValue = "";
            setEpaddVersion();
            setMode(mode);
            setJavaVersion();
            setOs();
        }

        private void setEpaddVersion()
        {
            linkingAgentIdentifierValue += ("ePADD - " + Version.version);
        }

        private void setOs()
        {
            linkingAgentIdentifierValue += (" - " + System.getProperty("os.name"));
        }

        private void setMode(String mode) { linkingAgentIdentifierValue += (" - " + mode);}

        private void setJavaVersion()
        {
            linkingAgentIdentifierValue += (" - running in " + System.getProperty("java.version"));
        }

        private static class LinkingAgentRole implements Serializable {
            @XmlAttribute
            private final String authority="eventRelatedAgentRole";
            @XmlAttribute
            private final String authorityURI="http://id.loc.gov/vocabulary/preservation/eventRelatedAgentRole";
            @XmlAttribute
            private final String valueURI="http://id.loc.gov/vocabulary/preservation/eventRelatedAgentRole/exe";
            @XmlValue
            private final String value = "executing program";

            private LinkingAgentRole()
            {
            }
        }

    }
}
