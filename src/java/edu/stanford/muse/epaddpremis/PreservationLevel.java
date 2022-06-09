package edu.stanford.muse.epaddpremis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class PreservationLevel implements Serializable {

    @XmlTransient
    private static final Logger log =  LogManager.getLogger(PreservationLevel.class);
    @XmlElement(name = "preservationLevelValue")
    private final String preservationLevelValue = "bit level";

    @XmlElement
    private final PreservationLevelRole preservationLevelRole = new PreservationLevelRole();

    @XmlElement
    @XmlJavaTypeAdapter(PreservationLevelRationaleTypeAdapter.class)
    private PreservationLevelRationaleType preservationLevelRationale = PreservationLevelRationaleType.NOT_DEFINED;

    @XmlElement
    private String preservationLevelDateAssigned = "";

   public PreservationLevel() {
    }

    public void setRationale(String preservationLevelRationale)
    {
        this.preservationLevelRationale = PreservationLevelRationaleType.fromString(preservationLevelRationale);
        if (this.preservationLevelRationale == null)
        {
            log.warn("Could not set preservationLevelRationaleType from String " + preservationLevelRationale);
        }
    }

    public void setRole(String preservationLevelRole)
    {
        this.preservationLevelRole.setRole(preservationLevelRole);
    }

    public void setPreservationLevelDateAssigned(String date) {
        this.preservationLevelDateAssigned = date;
    }

    public void setPreservationLevelDateAssignedToToday()
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        this.preservationLevelDateAssigned = ZonedDateTime.now().format(formatter);
    }
    public enum PreservationLevelRoleType {
        REQUIREMENT("requirement"), INTENTION("intention"), CAPABILITY("capability"), NOT_DEFINED("");

        private final String preservationLevelRoleType;

        PreservationLevelRoleType(String preservationLevelRoleType) {
            this.preservationLevelRoleType = preservationLevelRoleType;
        }

        public static PreservationLevelRoleType fromString(String text) {
            for (PreservationLevelRoleType p : PreservationLevelRoleType.values()) {
                if (p.preservationLevelRoleType.equalsIgnoreCase(text)) {
                    return p;
                }
            }
            return null;
        }

        public String toString()
        {
            return preservationLevelRoleType;
        }
    }

    private static class PreservationLevelRoleTypeAdapter extends XmlAdapter<String, PreservationLevelRoleType> implements Serializable
    {
        public String marshal(PreservationLevelRoleType roleType) {
            return roleType.toString();
        }
        public PreservationLevelRoleType unmarshal(String val) {
            return PreservationLevelRoleType.fromString(val);
        }
    }

    public enum PreservationLevelRationaleType {
        USER_PAID("user payed"), LEGISLATION("legislation"), PRESERVATION_POLICY("preservation policy"), NOT_DEFINED("");

        private final String preservationLevelRationaleType;

        PreservationLevelRationaleType(String preservationLevelRationaleType) {
            this.preservationLevelRationaleType = preservationLevelRationaleType;
        }

        public static PreservationLevelRationaleType fromString(String text) {
            for (PreservationLevelRationaleType p : PreservationLevelRationaleType.values()) {
                if (p.preservationLevelRationaleType.equalsIgnoreCase(text)) {
                    return p;
                }
            }
            return NOT_DEFINED;
        }

        public String toString()
        {
            return preservationLevelRationaleType;
        }
    }

    private static class PreservationLevelRationaleTypeAdapter extends XmlAdapter<String, PreservationLevelRationaleType> implements Serializable
    {
        public String marshal(PreservationLevelRationaleType reationalType) {
            return reationalType.toString();
        }
        public PreservationLevelRationaleType unmarshal(String val) {
            return PreservationLevelRationaleType.fromString(val);
        }
    }

    static class PreservationLevelRole implements Serializable {
        @XmlAttribute
        private final String authority = "preservationLevelRole";
        @XmlAttribute
        private final String authorityURI = "http://id.loc.gov/vocabulary/preservation/preservationLevelRole";
        @XmlAttribute
        private final String valueURI = "http://id.loc.gov/vocabulary/preservation/preservationLevelRole/req";

        @XmlValue
        @XmlJavaTypeAdapter(PreservationLevelRoleTypeAdapter.class)
        private PreservationLevelRoleType preservationLevelRoleType;

        private PreservationLevelRole() {
            this.preservationLevelRoleType = PreservationLevelRoleType.NOT_DEFINED;
        }

        private void setRole(String preservationLevelRole) {
            this.preservationLevelRoleType = PreservationLevelRoleType.fromString(preservationLevelRole);
            if (this.preservationLevelRoleType == null) {
                log.warn("Could not set preservationLevelRoleType from null String");
            }
        }
    }
}
