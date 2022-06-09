package edu.stanford.muse.epaddpremis;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class PremisRights implements Serializable {

    @XmlAttribute
    private final String xmlID = "AAAAG";
    @XmlAttribute
    private final String version = "3.0";
    @XmlElement
    private final RightsExtension rightsExtension = new RightsExtension();

    @XmlElement
    private final RightsStatement rightsStatement = new RightsStatement();




    public RightsExtension.StatuteInformation getStatuteInformation() {
        return rightsExtension.statuteInformation;
    }

    public RightsExtension getRightsExtension()
    {
        return rightsExtension;
    }
    protected void setRightsStatementIdentifierType(String type) {
        this.rightsStatement.rightsStatementIdentifier.rightsStatementIdentifierType = type;
    }

    protected void setRightsStatementIdentifierValue(String value) {
        this.rightsStatement.rightsStatementIdentifier.rightsStatementIdentifierValue = value;
    }

    private static class RightsStatement implements Serializable {
        @XmlElement
        RightsStatementIdentifier rightsStatementIdentifier = new RightsStatementIdentifier();
        @XmlElement
        private final String rightsBasis = "";
        private RightsStatement() {
        }

        private static class RightsStatementIdentifier implements Serializable {
            @XmlElement
            private String rightsStatementIdentifierType = "";

            @XmlElement
            private String rightsStatementIdentifierValue = "";

        }
    }

    public static class RightsExtension implements Serializable {
        @XmlElement
        private final StatuteInformation statuteInformation = new StatuteInformation();

        public StatuteInformation getStatuteInformation()
        {
            return statuteInformation;
        }

        public static class StatuteInformation implements Serializable {

            @XmlElement
            private String statuteJurisdiction = "";

            @XmlElement
            private final String statuteCitation = "";

            @XmlElement
            private final StatuteDocumentationIdentifier statuteDocumentationIdentifier = new StatuteDocumentationIdentifier();

            public void setStatuteJurisdiction(String statuteJurisdiction) {
                this.statuteJurisdiction = statuteJurisdiction;
            }

            protected void setStatuteDocumentationIdentifierType(String statuteDocumentationIdentifierType) {
                this.statuteDocumentationIdentifier.statuteDocumentationIdentifierType = statuteDocumentationIdentifierType;
            }

            protected void setStatuteDocumentationIdentifierValue(String statuteDocumentationIdentifierValue) {
                this.statuteDocumentationIdentifier.statuteDocumentationIdentifierValue = statuteDocumentationIdentifierValue;
            }

            protected void setStatuteDocumentationRole(String statuteDocumentationRole) {
                this.statuteDocumentationIdentifier.statuteDocumentationRole = statuteDocumentationRole;
            }

            private static class StatuteDocumentationIdentifier implements Serializable {
                @XmlElement
                private String statuteDocumentationIdentifierType = "";
                @XmlElement
                private String statuteDocumentationIdentifierValue = "";
                @XmlElement
                private String statuteDocumentationRole = "";


            }
        }

    }
}
