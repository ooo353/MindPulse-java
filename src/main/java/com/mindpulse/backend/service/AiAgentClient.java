package com.mindpulse.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AiAgentClient {

    @Value("${ai.service.base-url}")
    private String aiServiceBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 15000;
    private static final int MAX_RETRIES = 2;

    public AiAgentClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> parseTask(String taskDescription) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                String url = aiServiceBaseUrl + "/task";

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("task_description", taskDescription);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

                log.debug("Calling AI parse service, url={}, input={}", url, taskDescription);
                String response = restTemplate.postForObject(url, request, String.class);

                JsonNode responseNode = objectMapper.readTree(response);

                Map<String, Object> result = new HashMap<>();
                result.put("title", responseNode.path("title").asText(""));
                result.put("description", responseNode.path("description").asText(""));
                result.put("due_date", responseNode.path("due_date").asText(""));
                result.put("priority", responseNode.path("priority").asText("medium"));
                result.put("category", responseNode.path("category").asText(""));

                log.info("AI parse successful: title={}, priority={}, category={}",
                        result.get("title"), result.get("priority"), result.get("category"));
                return result;

            } catch (Exception e) {
                log.warn("AI service call failed (attempt {}/{}): {}", attempt + 1, MAX_RETRIES, e.getMessage());
                if (attempt == MAX_RETRIES - 1) {
                    log.error("AI service call ultimately failed, returning fallback values");
                    return buildFallback(taskDescription);
                }
            }
        }
        return buildFallback(taskDescription);
    }

    private Map<String, Object> buildFallback(String taskDescription) {
        Map<String, Object> fallback = new HashMap<>();
        String title = taskDescription.length() > 50 ? taskDescription.substring(0, 50) + "..." : taskDescription;
        fallback.put("title", title);
        fallback.put("description", taskDescription);
        fallback.put("due_date", "");
        fallback.put("priority", "medium");
        fallback.put("category", "");
        return fallback;
    }

    public Map<String, Object> generateSummary(String noteContent) {
        try {
            String url = aiServiceBaseUrl + "/generate_summary";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("note_content", noteContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(url, request, String.class);

            JsonNode responseNode = objectMapper.readTree(response);

            Map<String, Object> result = new HashMap<>();
            result.put("title", responseNode.path("title").asText(""));
            result.put("summary", responseNode.path("summary").asText(""));
            result.put("tags", responseNode.path("tags").asText(""));

            return result;
        } catch (Exception e) {
            log.error("AI summary generation failed: {}", e.getMessage());

            Map<String, Object> fallback = new HashMap<>();
            fallback.put("title", "Auto-generated Title");
            fallback.put("summary", "Could not generate summary due to AI service error");
            fallback.put("tags", "");
            return fallback;
        }
    }
}
