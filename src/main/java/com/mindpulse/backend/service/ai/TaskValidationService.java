package com.mindpulse.backend.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TaskValidationService {

    private static final Logger log = LoggerFactory.getLogger(TaskValidationService.class);
    private static final Set<String> VALID_PRIORITIES = Set.of("high", "medium", "low");
    private static final int MAX_TITLE_LENGTH = 200;

    /**
     * 校验 AI 返回的解析数据，返回错误列表，空列表表示校验通过
     */
    public List<String> validate(Map<String, Object> parsedData) {
        List<String> errors = new ArrayList<>();

        // title 校验
        Object titleObj = parsedData.get("title");
        if (titleObj == null || titleObj.toString().isBlank()) {
            errors.add("任务标题不能为空");
        } else if (titleObj.toString().length() > MAX_TITLE_LENGTH) {
            errors.add("任务标题长度超过限制 (" + titleObj.toString().length() + " > " + MAX_TITLE_LENGTH + ")");
        }

        // priority 校验
        Object priorityObj = parsedData.get("priority");
        String priority = priorityObj != null ? priorityObj.toString().toLowerCase() : "medium";
        if (!VALID_PRIORITIES.contains(priority)) {
            log.warn("无效优先级 '{}'，回退为 medium", priority);
            parsedData.put("priority", "medium");
        } else {
            parsedData.put("priority", priority);
        }

        // due_date 校验（可选字段）
        Object dueDateObj = parsedData.get("due_date");
        if (dueDateObj != null && !dueDateObj.toString().isBlank()) {
            String dueDateStr = dueDateObj.toString();
            try {
                if (dueDateStr.contains("T")) {
                    LocalDateTime.parse(dueDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } else {
                    LocalDateTime.parse(dueDateStr.replace(" ", "T"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            } catch (DateTimeParseException e) {
                log.warn("无法解析截止时间 '{}'，将设为 null", dueDateStr);
                parsedData.put("due_date", "");
            }
        }

        // category 校验：确保是逗号分隔的字符串
        Object categoryObj = parsedData.get("category");
        if (categoryObj != null && !categoryObj.toString().isBlank()) {
            String category = categoryObj.toString().trim();
            category = category.replaceAll("\\s*,\\s*", ",").replaceAll("^,+|,+$", "");
            parsedData.put("category", category);
        }

        if (errors.isEmpty()) {
            log.debug("AI 解析数据校验通过: title={}", parsedData.get("title"));
        } else {
            log.warn("AI 解析数据校验失败: {}", errors);
        }
        return errors;
    }

    /**
     * 校验并返回通过/失败标志
     */
    public boolean isValid(Map<String, Object> parsedData) {
        return validate(parsedData).isEmpty();
    }
}
