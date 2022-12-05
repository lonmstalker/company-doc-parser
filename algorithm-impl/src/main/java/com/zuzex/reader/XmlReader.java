package com.zuzex.reader;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Map;

public interface XmlReader {
  Map<String, Object> readMap(final byte[] bytes) throws IOException;

  JsonNode readNode(final byte[] bytes) throws IOException;

  <T> T readObject(final byte[] bytes) throws IOException;

  JsonNode catchRead(final byte[] bytes);

  Map<String, Object> catchReadMap(final byte[] bytes);
}
