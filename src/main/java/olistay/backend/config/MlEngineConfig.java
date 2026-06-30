package olistay.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Wires a dedicated RestClient bean for calling the FastAPI AI Engine
 * (main.py). Kept separate from any other RestClient/RestTemplate beans
 * that might exist for other integrations, since this one carries a
 * specific base URL and longer read timeout tuned for ML inference calls.
 */
@Configuration
public class MlEngineConfig {

    @Value("${ml.engine.base-url}")
    private String baseUrl;

    @Value("${ml.engine.connect-timeout-ms}")
    private long connectTimeoutMs;

    @Value("${ml.engine.read-timeout-ms}")
    private long readTimeoutMs;

    @Bean
    public RestClient mlEngineRestClient() {
        var requestFactorySettings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs));

        var requestFactory = ClientHttpRequestFactoryBuilder.detect()
                .build(requestFactorySettings);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}