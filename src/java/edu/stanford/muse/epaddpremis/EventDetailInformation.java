package edu.stanford.muse.epaddpremis;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.io.Serializable;

    @XmlType
    public class EventDetailInformation implements Serializable {
        private static final long serialVersionUID = 1L;
        @XmlElement
        private String eventDetail;

        @XmlElement
        private EventDetailExtension eventDetailExtension;

        public EventDetailInformation() {}

        public EventDetailInformation(String eventDetail) {
            this.eventDetail = eventDetail;
        }

        protected EventDetailInformation(String eventDetail, String title, String id, String folder, String date, String scopeAndContent, String rightsAndConditions, String notes) {
            this.eventDetail = eventDetail;
            this.eventDetailExtension = new EventDetailExtension(title, id, folder, date, scopeAndContent, rightsAndConditions, notes);
        }


            public void setEventDetail(String eventDetail) {
            this.eventDetail = eventDetail;
        }
}