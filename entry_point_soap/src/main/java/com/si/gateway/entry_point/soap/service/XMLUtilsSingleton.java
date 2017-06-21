package com.si.gateway.entry_point.soap.service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XMLUtilsSingleton {

    private static XMLUtilsSingleton instance;
    private DocumentBuilderFactory factory;

    private XMLUtilsSingleton() throws ParserConfigurationException {
        factory = DocumentBuilderFactory.newInstance();
        // f.setValidating(false);
    }

    public static XMLUtilsSingleton getInstance() throws ParserConfigurationException {
        if (instance == null) {
            instance = new XMLUtilsSingleton();
        }
        return instance;
    }

    public DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        return factory.newDocumentBuilder();
    }
}
