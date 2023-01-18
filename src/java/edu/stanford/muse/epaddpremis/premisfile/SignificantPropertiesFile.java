package edu.stanford.muse.epaddpremis.premisfile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class SignificantPropertiesFile implements Serializable {

    @XmlTransient
    private static final Logger log = LogManager.getLogger(SignificantPropertiesFile.class);

    @XmlElement

    @XmlJavaTypeAdapter(SignificantPropertiesTypeAdapter.class)
    private PropertyType significantPropertiesType;

    @XmlElement
    private String significantPropertiesValue;

    public SignificantPropertiesFile(PropertyType significantPropertiesType, String significantPropertiesValue) {
        this.significantPropertiesType = significantPropertiesType;
        if (this.significantPropertiesType == PropertyType.NOT_RECOGNIZED) {
            log.warn("Significant property not recognized: " + significantPropertiesType + " (value = " + significantPropertiesValue + ")");
        }
        this.significantPropertiesValue = significantPropertiesValue;
    }

    public SignificantPropertiesFile() {
    }

    public static Set<SignificantPropertiesFile> getInitialSet() {
        Set<SignificantPropertiesFile> set = new HashSet<>();
        PropertyType[] propertyTypes = PropertyType.values();
        for (PropertyType p : propertyTypes) {
            if (p != PropertyType.NOT_RECOGNIZED)
            {
                set.add(new SignificantPropertiesFile(p, ""));
            }
        }
        return set;
    }

    void setValue(String value) {
        this.significantPropertiesValue = value;
    }

    public PropertyType getType() {
        return significantPropertiesType;
    }

    public void setType(String type) {
        this.significantPropertiesType = SignificantPropertiesFile.PropertyType.fromString(type);
    }

    enum PropertyType {
        ENVIRONMENT_NOTE("environmentNote"), SOFTWARE_NAME("softwareName"), SOFTWARE_VERSION("softwareVersion"),
        NOT_RECOGNIZED("not recognized");

        // retain only up to first semi-colon; often ct i
        private final String propertyType;

        PropertyType(String propertyType) {
            this.propertyType = propertyType;
        }

        public static PropertyType fromString(String text) {
            for (PropertyType p : PropertyType.values()) {
                if (p.propertyType.equalsIgnoreCase(text)) {
                    return p;
                }
            }
            return NOT_RECOGNIZED;
        }

        public String toString() {
            return propertyType;
        }
    }
    public static class SignificantPropertiesTypeAdapter extends XmlAdapter<String, PropertyType> implements Serializable {
        public String marshal(PropertyType propertyType) {
            return propertyType.toString();
        }
        public PropertyType unmarshal(String val) {
            return PropertyType.fromString(val);
        }
    }
}

