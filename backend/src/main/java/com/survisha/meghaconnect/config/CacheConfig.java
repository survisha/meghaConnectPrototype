package com.survisha.meghaconnect.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Configuration
@EnableCaching
@EnableConfigurationProperties(RedisCacheProperties.class)
@Slf4j
public class CacheConfig implements CachingConfigurer {

    public static final String REFERENCE_DATA = "referenceData";
    public static final String DEPARTMENTS = "departments";
    public static final String DISTRICTS = "districts";
    public static final String MANDALS = "mandals";
    public static final String SCHEMES = "schemes";
    public static final String AGENDA_TYPES = "agendaTypes";
    public static final String ROLES = "roles";
    public static final String USER_AUTHORIZATION = "userAuthorization";
    public static final String CITIZEN_PROFILES = "citizenProfiles";
    public static final String CITIZEN_PROFILES_BY_MOBILE = "citizenProfilesByMobile";

    @Bean
    @Primary
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> connectionFactories,
                                     RedisCacheProperties properties,
                                     GenericJackson2JsonRedisSerializer cacheRedisSerializer) {
        if (!properties.isEnabled()) {
            log.info("Redis business caches are disabled; using NoOpCacheManager");
            return new NoOpCacheManager();
        }

        RedisConnectionFactory connectionFactory = connectionFactories.getIfAvailable();
        if (connectionFactory == null) {
            throw new IllegalStateException("Redis caching is enabled but no RedisConnectionFactory is configured");
        }

        RedisCacheConfiguration defaults = configuration(
                properties.getReferenceDataTtl(), properties.getEnvironment(), cacheRedisSerializer);
        Map<String, RedisCacheConfiguration> caches = new LinkedHashMap<>();
        caches.put(REFERENCE_DATA, configuration(properties.getReferenceDataTtl(), properties.getEnvironment(), cacheRedisSerializer));
        caches.put(DEPARTMENTS, configuration(properties.getDepartmentTtl(), properties.getEnvironment(), cacheRedisSerializer));
        caches.put(DISTRICTS, configuration(properties.getDistrictTtl(), properties.getEnvironment(), cacheRedisSerializer));
        caches.put(MANDALS, configuration(properties.getMandalTtl(), properties.getEnvironment(), cacheRedisSerializer));
        caches.put(SCHEMES, configuration(properties.getSchemeTtl(), properties.getEnvironment(), cacheRedisSerializer));
        caches.put(AGENDA_TYPES, configuration(properties.getAgendaTypeTtl(), properties.getEnvironment(), cacheRedisSerializer));
        caches.put(ROLES, configuration(properties.getReferenceDataTtl(), properties.getEnvironment(), cacheRedisSerializer));
        caches.put(USER_AUTHORIZATION, configuration(properties.getUserTtl(), properties.getEnvironment(), cacheRedisSerializer));
        caches.put(CITIZEN_PROFILES, configuration(properties.getCitizenProfileTtl(), properties.getEnvironment(), cacheRedisSerializer));
        caches.put(CITIZEN_PROFILES_BY_MOBILE,
                configuration(properties.getCitizenProfileTtl(), properties.getEnvironment(), cacheRedisSerializer));

        log.info("Redis business caches enabled environment={} cacheCount={}",
                safeEnvironment(properties.getEnvironment()), caches.size());
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(caches)
                .transactionAware()
                .build();
    }

    @Bean
    public GenericJackson2JsonRedisSerializer cacheRedisSerializer(ObjectMapper applicationObjectMapper) {
        ObjectMapper mapper = applicationObjectMapper.copy();
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.survisha.meghaconnect.")
                        .allowIfSubType("java.lang.")
                        .allowIfSubType("java.time.")
                        .allowIfSubType("java.util.")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    private RedisCacheConfiguration configuration(Duration ttl, String environment,
                                                   GenericJackson2JsonRedisSerializer serializer) {
        String prefix = "meghaconnect:" + safeEnvironment(environment) + ":cache:";
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> prefix + cacheName + "::")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        serializer));
    }

    private String safeEnvironment(String value) {
        String normalized = value == null ? "local" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("[a-z0-9_-]+") ? normalized : "local";
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                warn("get", exception, cache);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                warn("put", exception, cache);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                warn("evict", exception, cache);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                warn("clear", exception, cache);
            }

            private void warn(String operation, RuntimeException exception, Cache cache) {
                log.warn("Redis business cache operation failed operation={} cache={} reason={}",
                        operation, cache.getName(), exception.getClass().getSimpleName());
            }
        };
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.additionalInterceptors((request, body, execution) -> {
            String requestId = RequestContextUtil.getRequestId();
            request.getHeaders().set(RequestContextUtil.REQUEST_ID_HEADER, requestId);
            String safeUri = RequestContextUtil.safeUri(request.getURI());
            long startTime = System.currentTimeMillis();
            log.info("External API request started method={} uri={}", request.getMethod(), safeUri);
            try {
                ClientHttpResponse response = execution.execute(request, body);
                log.info("External API request completed method={} uri={} status={} durationMs={}",
                        request.getMethod(), safeUri, response.getStatusCode().value(),
                        System.currentTimeMillis() - startTime);
                return response;
            } catch (Exception e) {
                log.error("External API request failed method={} uri={} durationMs={}",
                        request.getMethod(), safeUri, System.currentTimeMillis() - startTime, e);
                throw e;
            }
        }).build();
    }
}
