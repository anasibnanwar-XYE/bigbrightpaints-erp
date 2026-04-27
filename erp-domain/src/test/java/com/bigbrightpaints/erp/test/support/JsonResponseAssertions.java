package com.bigbrightpaints.erp.test.support;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonResponseAssertions {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
  private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_MAP_TYPE =
      new ParameterizedTypeReference<>() {};
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<Map<String, Object>>> MAP_LIST_TYPE =
      new TypeReference<>() {};

  private JsonResponseAssertions() {}

  public static ParameterizedTypeReference<Map<String, Object>> responseMapType() {
    return RESPONSE_MAP_TYPE;
  }

  public static Map<String, Object> responseDataMap(ResponseEntity<Map<String, Object>> response) {
    return mapValue(responseData(response));
  }

  public static List<Map<String, Object>> responseDataMapList(
      ResponseEntity<Map<String, Object>> response) {
    return mapList(responseData(response));
  }

  public static Map<String, Object> mapValue(Object value) {
    requireType(value, Map.class, "JSON object");
    return OBJECT_MAPPER.convertValue(value, MAP_TYPE);
  }

  public static List<Map<String, Object>> mapList(Object value) {
    requireType(value, List.class, "JSON object list");
    return OBJECT_MAPPER.convertValue(value, MAP_LIST_TYPE);
  }

  private static Object responseData(ResponseEntity<Map<String, Object>> response) {
    Map<String, Object> body = response.getBody();
    if (body == null) {
      throw new IllegalArgumentException("Expected response body");
    }
    if (!body.containsKey("data")) {
      throw new IllegalArgumentException("Expected response body to contain data");
    }
    return body.get("data");
  }

  private static void requireType(Object value, Class<?> requiredType, String label) {
    if (!requiredType.isInstance(value)) {
      String actualType = value == null ? "null" : value.getClass().getName();
      throw new IllegalArgumentException("Expected " + label + " but got " + actualType);
    }
  }
}
