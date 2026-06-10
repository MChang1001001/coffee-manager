package com.example.coffeebean.coffee;

import com.example.coffeebean.common.BusinessException;
import com.example.coffeebean.common.ErrorCode;
import com.example.coffeebean.config.DeepSeekProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CoffeeAiSummaryClient {

    private static final String SYSTEM_PROMPT = """
            你是一个咖啡品鉴助手。请根据用户提供的咖啡豆基础信息、评价记录和冲煮记录，生成一份中文咖啡豆评测总结。请只输出 json，不要输出 Markdown，不要编造不存在的数据。

            输出 JSON：
            {
              "summaryTitle": "",
              "flavorSummary": "",
              "brewSuggestion": "",
              "repurchaseIntention": "",
              "summaryText": ""
            }

            规则：
            1. 不要编造不存在的风味。
            2. 数据不足时说明“记录较少，结论偏初步”。
            3. repurchaseIntention 只能是：未决定 / 会回购 / 看情况 / 不回购。
            4. 文风自然，适合个人咖啡手账，不要营销味。
            5. 不要输出 API key 或敏感信息。
            """;
    private static final Set<String> REPURCHASE_OPTIONS = Set.of("未决定", "会回购", "看情况", "不回购");
    private static final String MISSING_API_KEY_MESSAGE = "AI 总结功能未配置 DeepSeek API Key。";
    private static final String AI_FAILED_MESSAGE = "AI 总结生成失败，请稍后重试。";

    private final DeepSeekProperties deepSeekProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CoffeeAiSummaryClient(DeepSeekProperties deepSeekProperties, ObjectMapper objectMapper) {
        this.deepSeekProperties = deepSeekProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public CoffeeSummaryDraftResponse generateSummary(String userPrompt) {
        if (!deepSeekProperties.isEnabled() || !StringUtils.hasText(deepSeekProperties.getApiKey())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, MISSING_API_KEY_MESSAGE);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(chatCompletionsUri())
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + deepSeekProperties.getApiKey().trim())
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(userPrompt), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, AI_FAILED_MESSAGE);
            }
            CoffeeSummaryDraftResponse draft = parseDraft(extractContent(response.body()));
            draft.setSummarySource("AI");
            draft.setSummaryGeneratedAt(LocalDateTime.now());
            return draft;
        } catch (BusinessException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, AI_FAILED_MESSAGE);
        } catch (IOException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, AI_FAILED_MESSAGE);
        }
    }

    private String buildRequestBody(String userPrompt) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", defaultIfBlank(deepSeekProperties.getModel(), "deepseek-chat"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt)));
        body.put("response_format", Map.of("type", "json_object"));
        body.put("temperature", 0.4);
        return objectMapper.writeValueAsString(body);
    }

    private URI chatCompletionsUri() {
        return URI.create(normalizeBaseUrl(deepSeekProperties.getBaseUrl()) + "/chat/completions");
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = defaultIfBlank(baseUrl, "https://api.deepseek.com");
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String extractContent(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (!contentNode.isTextual() || !StringUtils.hasText(contentNode.asText())) {
            throw new IllegalArgumentException("DeepSeek response content is empty");
        }
        return contentNode.asText();
    }

    private CoffeeSummaryDraftResponse parseDraft(String content) throws IOException {
        JsonNode root = objectMapper.readTree(extractJsonContent(content));
        CoffeeSummaryDraftResponse draft = new CoffeeSummaryDraftResponse();
        draft.setSummaryTitle(normalize(root.path("summaryTitle").asText(null)));
        draft.setFlavorSummary(normalize(root.path("flavorSummary").asText(null)));
        draft.setBrewSuggestion(normalize(root.path("brewSuggestion").asText(null)));
        draft.setRepurchaseIntention(normalizeRepurchase(root.path("repurchaseIntention").asText(null)));
        draft.setSummaryText(normalize(root.path("summaryText").asText(null)));
        return draft;
    }

    private String extractJsonContent(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }

    private String normalizeRepurchase(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return REPURCHASE_OPTIONS.contains(normalized) ? normalized : "未决定";
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized == null ? defaultValue : normalized;
    }
}
