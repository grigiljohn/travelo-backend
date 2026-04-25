package com.travelo.discoveryservice.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiConfiguration {

    @Bean(name = "openAiWebClient")
    public WebClient openAiWebClient(OpenAiProperties props, WebClient.Builder webClientBuilder) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(props.getReadTimeoutMs()));
        return webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://api.openai.com")
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                        .build())
                .build();
    }
}
