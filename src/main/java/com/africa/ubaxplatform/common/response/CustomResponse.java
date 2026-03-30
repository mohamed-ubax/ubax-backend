package com.africa.ubaxplatform.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

@Getter
@Setter
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomResponse {

  private String status;
  private Integer statusCode;
  private String message;
  private Object data;

  public CustomResponse(String status, int statusCode, String message, Object data) {
    this.status = status;
    this.statusCode = statusCode;
    this.message = message;
    this.data = data instanceof Page<?> page ? convertToResponse(page) : data;
  }

  private static Map<String, Object> convertToResponse(Page<?> page) {
    Map<String, Object> response = new HashMap<>();
    response.put("total-pages", page.getTotalPages());
    response.put("page", page.getNumber());
    response.put("total-items", page.getTotalElements());
    response.put("sortDir", page.getSort());
    response.put("perPage", page.getSize());
    response.put("results", page.getContent());
    response.put("isFirst", page.isFirst());
    response.put("isLast", page.isLast());
    return response;
  }
}
