package edu.stanford.muse.epaddpremis;

import javax.xml.bind.annotation.XmlElement;

public class Agent {

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

    private static class AgentIdentifier {

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
