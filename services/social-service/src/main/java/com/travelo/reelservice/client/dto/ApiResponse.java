package com.travelo.reelservice.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * API Response wrapper from post-service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private Boolean success;
    private String message;
    private T data;
    private String timestamp;
    private String errorCode;
    private Map<String, Object> details;

    // Getters and Setters
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

