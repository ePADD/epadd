package edu.stanford.muse.epaddpremis;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class PremisRights implements Serializable {

    private static final long serialVersionUID = 2484804048125390122L;
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
        private static final long serialVersionUID = 5698039281836318317L;
        @XmlElement
        RightsStatementIdentifier rightsStatementIdentifier = new RightsStatementIdentifier();
        @XmlElement
        private final String rightsBasis = "";
        private RightsStatement() {
        }

        private static class RightsStatementIdentifier implements Serializable {
            private static final long serialVersionUID = -8878949886761368281L;
            @XmlElement
            private String rightsStatementIdentifierType = "";

            @XmlElement
            private String rightsStatementIdentifierValue = "";

        }
    }

    public static class RightsExtension implements Serializable {
        private static final long serialVersionUID = -8755643207947803030L;
        @XmlElement
        private final StatuteInformation statuteInformation = new StatuteInformation();

        public StatuteInformation getStatuteInformation()
        {
            return statuteInformation;
        }

        public static class StatuteInformation implements Serializable {

            private static final long serialVersionUID = -4680242431123009162L;
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
                private static final long serialVersionUID = -6450831195687191660L;
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
