package dev.ixixpercent.cache.cache_tester.debug;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

@Slf4j
@Component
public class CacheProviderChecker {

  @PostConstruct
  public void checkCachingProviders() {
    log.trace("Available Caching Providers:");
    for (CachingProvider provider : Caching.getCachingProviders()) {
      log.trace("{}", provider.getClass().getName());
    }
  }
}
