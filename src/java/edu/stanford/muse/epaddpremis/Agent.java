package edu.stanford.muse.epaddpremis;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Agent implements Serializable {

    @XmlElement(name="agentIdentifier")
    private AgentIdentifier agentIdentifier;

    @XmlElement(name="agentName")
    private String agentName;

    @XmlElement(name="agentType")
    private String agentType;

    @XmlElement(name="agentVersion")
    private String agentVersion;

    public Agent()
    {
    }

    public Agent(String agentIdentifierValue, String name, String type, String version)
    {
        agentIdentifier = new AgentIdentifier(agentIdentifierValue);
        agentName = name;
        agentType = type;
        agentVersion = version;
    }

    private static class AgentIdentifier implements Serializable {

        @XmlElement(name="agentIdentifierType")
        private final String agentIdentifierType = "local";

        @XmlElement(name="agentIdentifierValue")
        private String agentIdentifierValue;

        private AgentIdentifier()
        {
        }

        private AgentIdentifier(String value) {
            agentIdentifierValue = value;
        }
    }
}
