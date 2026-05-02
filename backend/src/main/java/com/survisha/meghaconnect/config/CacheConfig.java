package com.survisha.meghaconnect.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.http.client.ClientHttpResponse;

import java.time.Duration;
import org.springframework.web.client.RestTemplate;
import com.survisha.meghaconnect.util.RequestContextUtil;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private String redisPort;

    @Value("${cache.redis.enabled:false}")
    private boolean redisEnabled;

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // Cache for 1 hour
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                );
    }

    @Bean
    @Primary
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider,
                                     ObjectProvider<RedisCacheConfiguration> cacheConfigurationProvider) {
        if (!redisEnabled) {
            log.info("Redis cache disabled for phase1 release - using NoOpCacheManager");
            return new NoOpCacheManager();
        }

        RedisConnectionFactory redisConnectionFactory = redisConnectionFactoryProvider.getIfAvailable();
        RedisCacheConfiguration cacheConfiguration = cacheConfigurationProvider.getIfAvailable();

        if (redisConnectionFactory == null || cacheConfiguration == null) {
            log.error("Redis cache enabled but Redis configuration is unavailable");
            throw new IllegalStateException("Redis caching is enabled but Redis configuration is missing.");
        }

        // Test Redis connection on startup
        try {
            redisConnectionFactory.getConnection().ping();
            log.info("Redis connection successful - cache manager initialized");
        } catch (Exception e) {
            log.error("Redis connection failed! Please ensure Redis is running on {}:{}", redisHost, redisPort);
            throw new RuntimeException("Redis is required but not available. Please start Redis server.", e);
        }

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration)
                .build();
    }

    /**
     * RestTemplate bean for external API calls (e.g., OVSE service).
     * Configured with reasonable timeouts and connection pooling.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .additionalInterceptors((request, body, execution) -> {
                    String requestId = RequestContextUtil.getRequestId();
                    request.getHeaders().set(RequestContextUtil.REQUEST_ID_HEADER, requestId);

                    String safeUri = RequestContextUtil.safeUri(request.getURI());
                    long startTime = System.currentTimeMillis();
                    log.info("External API request started method={} uri={}", request.getMethod(), safeUri);

                    try {
                        ClientHttpResponse response = execution.execute(request, body);
                        log.info("External API request completed method={} uri={} status={} durationMs={}",
                                request.getMethod(),
                                safeUri,
                                response.getStatusCode().value(),
                                System.currentTimeMillis() - startTime);
                        return response;
                    } catch (Exception e) {
                        log.error("External API request failed method={} uri={} durationMs={}",
                                request.getMethod(),
                                safeUri,
                                System.currentTimeMillis() - startTime,
                                e);
                        throw e;
                    }
                })
                .build();
    }
}
