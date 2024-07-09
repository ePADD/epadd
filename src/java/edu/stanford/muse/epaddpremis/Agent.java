package edu.stanford.muse.epaddpremis;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Agent implements Serializable {

    private static final long serialVersionUID = -8361916629404885272L;
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

        private static final long serialVersionUID = 6080790205853431994L;
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
