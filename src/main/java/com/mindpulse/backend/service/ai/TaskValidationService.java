package com.mindpulse.backend.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class TaskValidationService {

    private static final Set<String> VALID_PRIORITIES = Set.of("high", "medium", "low");
    private static final int MAX_TITLE_LENGTH = 200;

    public List<String> validate(Map<String, Object> parsedData) {
        List<String> errors = new ArrayList<>();

        Object titleObj = parsedData.get("title");
        if (titleObj == null || titleObj.toString().isBlank()) {
            errors.add("Task title is required");
        } else if (titleObj.toString().length() > MAX_TITLE_LENGTH) {
            errors.add("Task title exceeds length limit (" + titleObj.toString().length() + " > " + MAX_TITLE_LENGTH + ")");
        }

        Object priorityObj = parsedData.get("priority");
        String priority = priorityObj != null ? priorityObj.toString().toLowerCase() : "medium";
        if (!VALID_PRIORITIES.contains(priority)) {
            log.warn("Invalid priority '{}', falling back to medium", priority);
            parsedData.put("priority", "medium");
        } else {
            parsedData.put("priority", priority);
        }

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
                log.warn("Cannot parse due date '{}', setting to null", dueDateStr);
                parsedData.put("due_date", "");
            }
        }

        Object categoryObj = parsedData.get("category");
        if (categoryObj != null && !categoryObj.toString().isBlank()) {
            String category = categoryObj.toString().trim();
            category = category.replaceAll("\\s*,\\s*", ",").replaceAll("^,+|,+$", "");
            parsedData.put("category", category);
        }

        if (errors.isEmpty()) {
            log.debug("AI parse data validation passed: title={}", parsedData.get("title"));
        } else {
            log.warn("AI parse data validation failed: {}", errors);
        }
        return errors;
    }

    public boolean isValid(Map<String, Object> parsedData) {
        return validate(parsedData).isEmpty();
    }
}
