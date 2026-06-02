package com.mindpulse.backend.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaskValidationServiceTest {

    private TaskValidationService service;

    @BeforeEach
    void setUp() {
        service = new TaskValidationService();
    }

    @Test
    @DisplayName("有效数据应校验通过")
    void validDataShouldPass() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "去图书馆还书");
        data.put("description", "去图书馆还书");
        data.put("due_date", "2026-05-24T15:00:00");
        data.put("priority", "high");
        data.put("category", "学习,紧急");

        List<String> errors = service.validate(data);
        assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("title 为空应报错")
    void emptyTitleShouldFail() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "");
        data.put("priority", "medium");

        List<String> errors = service.validate(data);
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("标题不能为空"));
    }

    @Test
    @DisplayName("title 为 null 应报错")
    void nullTitleShouldFail() {
        Map<String, Object> data = new HashMap<>();
        data.put("priority", "medium");

        List<String> errors = service.validate(data);
        assertFalse(errors.isEmpty());
    }

    @Test
    @DisplayName("无效优先级应回退为 medium")
    void invalidPriorityShouldFallback() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "测试任务");
        data.put("priority", "critical");

        service.validate(data);
        assertEquals("medium", data.get("priority"));
    }

    @Test
    @DisplayName("有效优先级应保留")
    void validPriorityShouldBePreserved() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "测试任务");
        data.put("priority", "LOW");

        service.validate(data);
        assertEquals("low", data.get("priority"));
    }

    @Test
    @DisplayName("category 中多余空格应被清理")
    void categoryWithExtraSpacesShouldBeCleaned() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "测试任务");
        data.put("priority", "medium");
        data.put("category", " 学习 , 工作 , 紧急 ");

        service.validate(data);
        assertEquals("学习,工作,紧急", data.get("category"));
    }

    @Test
    @DisplayName("isValid 返回 true 对有效数据")
    void isValidShouldReturnTrue() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "测试任务");
        data.put("priority", "medium");

        assertTrue(service.isValid(data));
    }

    @Test
    @DisplayName("isValid 返回 false 对无效数据")
    void isValidShouldReturnFalse() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "");
        data.put("priority", "medium");

        assertFalse(service.isValid(data));
    }

    @Test
    @DisplayName("title 超过 200 字符应报错")
    void titleTooLongShouldFail() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "A".repeat(201));
        data.put("priority", "medium");

        List<String> errors = service.validate(data);
        assertFalse(errors.isEmpty());
    }
}
