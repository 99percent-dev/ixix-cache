package dev.ixixpercent.cache.cache_tester.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

@Slf4j
@Configuration
@EnableCaching
public class ProviderConfiguration {

  @Bean
  public org.springframework.cache.CacheManager jCacheManager() {
    CachingProvider cachingProvider = Caching.getCachingProvider();
    CacheManager cacheManager = cachingProvider.getCacheManager();
    log.trace("Created cache manager {}", cacheManager);
    return new JCacheCacheManager(cacheManager);
  }
}
