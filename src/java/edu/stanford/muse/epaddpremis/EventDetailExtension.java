package edu.stanford.muse.epaddpremis;

import jakarta.xml.bind.annotation.XmlElement;

import java.io.Serializable;

public class EventDetailExtension implements Serializable {
    private static final long serialVersionUID	= 1L;
    @XmlElement
    private String id;
    @XmlElement
    private String title;
    @XmlElement
    private String folder;
    @XmlElement
    private String date;
    @XmlElement
    private String scopeAndContent;
    @XmlElement
    private String rightsAndConditions;
    @XmlElement
    private String notes;

    protected EventDetailExtension(){}

    protected EventDetailExtension(String title, String id, String folder, String date, String scopeAndContent, String rightsAndConditions, String notes) {
        this.title = title;
        this.id = id;
        this.date = date;
        this.folder = folder;
        this.scopeAndContent = scopeAndContent;
        this.rightsAndConditions = rightsAndConditions;
        this.notes = notes;
    }
}
