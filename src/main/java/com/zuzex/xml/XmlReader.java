package com.zuzex.xml;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public interface XmlReader {
    JsonNode readNode(final byte[] bytes) throws IOException;
    <T> T readObject(final byte[] bytes) throws IOException;

}
