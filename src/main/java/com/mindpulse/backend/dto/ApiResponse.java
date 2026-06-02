package com.mindpulse.backend.dto;

import java.time.LocalDateTime;

/**
 * 通用API响应类
 * 
 * @param <T> 响应数据类型
 */
public class ApiResponse<T> {
    
    private boolean success;
    private int code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ApiResponse(boolean success, int code, String message) {
        this();
        this.success = success;
        this.code = code;
        this.message = message;
    }
    
    public ApiResponse(boolean success, int code, String message, T data) {
        this(success, code, message);
        this.data = data;
    }
    
    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, 200, "Success");
    }
    
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, 200, message);
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "Success", data);
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, 200, message, data);
    }
    
    /**
     * 成功响应，带特定HTTP状态码
     */
    public static <T> ApiResponse<T> success(int code, String message) {
        return new ApiResponse<>(true, code, message);
    }
    
    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return new ApiResponse<>(true, code, message, data);
    }
    
    /**
     * 客户端错误响应 (4xx)
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(false, 400, message);
    }
    
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(false, 401, message);
    }
    
    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(false, 403, message);
    }
    
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(false, 404, message);
    }
    
    /**
     * 服务器错误响应 (5xx)
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, 500, message);
    }
    
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, code, message);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}