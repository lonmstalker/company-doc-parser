package com.zuzex.xml.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.zuzex.xml.XmlReader;

import java.io.IOException;

public class JacksonXmlReader implements XmlReader {
    private static final XmlMapper XML_READER = new XmlMapper();
    private static final ObjectReader OBJECT_READER = XML_READER.reader();

    @Override
    public JsonNode readNode(final byte[] bytes) throws IOException {
        return OBJECT_READER.readTree(bytes);
    }

    @Override
    public <T> T readObject(final byte[] bytes) throws IOException {
        return OBJECT_READER.readValue(bytes);
    }
}
