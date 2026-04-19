package com.travelo.momentsservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional OpenAI enrichment for {@code /api/v1/moments/ai/suggest}. Timeline JSON always comes from
 * {@link com.travelo.momentsservice.ai.MomentAiTimelinePlanner}; the model only refines caption/tags/filter.
 */
@ConfigurationProperties(prefix = "moments.ai.openai")
public class MomentsAiOpenAiProperties {

    /**
     * When false or API key blank, enrichment is skipped (no outbound calls).
     */
    private boolean enabled = false;

    private String apiKey = "";

    private String baseUrl = "https://api.openai.com/v1";

    private String model = "gpt-4o-mini";

    private int timeoutSeconds = 15;

    private int maxTokens = 400;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
}
