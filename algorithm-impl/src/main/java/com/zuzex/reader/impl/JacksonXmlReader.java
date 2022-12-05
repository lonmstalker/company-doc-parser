package com.zuzex.reader.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.zuzex.reader.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class JacksonXmlReader implements XmlReader {
  private static final XmlMapper XML_READER = new XmlMapper();
  private static final ObjectReader OBJECT_READER = XML_READER.reader();

  @Override
  public Map<String, Object> readMap(final byte[] bytes) throws IOException {
    return readObject(bytes);
  }

  @Override
  public JsonNode readNode(final byte[] bytes) throws IOException {
    return OBJECT_READER.readTree(bytes);
  }

  @Override
  public <T> T readObject(final byte[] bytes) throws IOException {
    return OBJECT_READER.readValue(bytes);
  }

  @Override
  public JsonNode catchRead(final byte[] bytes) {
    try {
      return this.readNode(bytes);
    } catch (final IOException e) {
      log.error("Error: ", e);
      return JsonNodeFactory.instance.nullNode();
    }
  }

  @Override
  public Map<String, Object> catchReadMap(final byte[] bytes) {
    try {
      return readMap(bytes);
    } catch (IOException e) {
      return Map.of();
    }
  }
}
