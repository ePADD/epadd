package edu.stanford.muse.epaddpremis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class SignificantProperties implements Serializable {

    @XmlTransient
    private static final Logger log = LogManager.getLogger(SignificantProperties.class);

    @XmlElement
    private String significantPropertiesType;

    @XmlElement
    private int significantPropertiesValue;

    public SignificantProperties(String significantPropertiesType, int significantPropertiesValue) {
        this.significantPropertiesType = significantPropertiesType;
        this.significantPropertiesValue = significantPropertiesValue;
    }

    public SignificantProperties() {
    }

    public static Set<String> initialise() {
        Set<String> initialProperties = new HashSet<>();
       // initialProperties.add("mbox file count");
        initialProperties.add("overall message_count");
        initialProperties.add("overall unique message_count");
        initialProperties.add("overall unique attachment count");
        initialProperties.add("overall unique attached email count");

        return initialProperties;
    }

    @XmlTransient
    protected String getType() {
        return significantPropertiesType;
    }

    protected void addValue(int value) {
        this.significantPropertiesValue += value;
    }

}

