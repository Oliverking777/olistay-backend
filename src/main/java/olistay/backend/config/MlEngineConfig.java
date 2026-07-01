package olistay.backend.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
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
        // Pin the underlying client to HTTP/1.1. The JDK HttpClient defaults to
        // HTTP/2 and, over cleartext http://, attempts an h2c upgrade
        // (Upgrade: h2c / HTTP2-Settings headers). Uvicorn — the ASGI server
        // running the FastAPI AI Engine — does not support h2c, and the request
        // BODY is silently dropped during that failed upgrade negotiation. The
        // engine then sees an empty body and every POST fails with a misleading
        //   422 {"loc":["body"],"input":null}
        // Forcing HTTP/1.1 sends a normal request with the body intact.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        // Omit null fields when serialising ML request DTOs. Every ML request
        // DTO models its fields as boxed nullable types, but the FastAPI
        // (Pydantic v2) models mark many fields as required or as
        // non-Optional-with-a-default. Pydantic v2 REJECTS an explicit JSON
        // null for a non-Optional field even when it has a default, so a single
        // unset column (e.g. gps_lat, length_m, savings_goal) sent as null
        // would trigger a 422. Dropping nulls lets Pydantic apply its own
        // defaults; genuinely-required fields still (correctly) 422 if absent.
        ObjectMapper mlObjectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        var jacksonConverter = new MappingJackson2HttpMessageConverter(mlObjectMapper);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(jacksonConverter);
                })
                .build();
    }
}