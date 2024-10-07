package dev.ixixpercent.cache.cache_tester.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "")
@Getter
@Setter
public class CachesProperties {
  private Map<String, CacheConfig> caches;
}
