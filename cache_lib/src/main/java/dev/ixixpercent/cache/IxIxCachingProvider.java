package dev.ixixpercent.cache;

import javax.cache.CacheManager;
import javax.cache.configuration.OptionalFeature;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class IxIxCachingProvider implements CachingProvider {

  public static final String CACHE_PROVIDER_URI = "ixix://cache";
  private final Map<ClassLoader, Map<URI, CacheManager>> cacheManagers = new ConcurrentHashMap<>();

  @Override
  public CacheManager getCacheManager(final URI uri, final ClassLoader classLoader, final Properties properties) {
    return cacheManagers
      .computeIfAbsent(classLoader, cl -> new ConcurrentHashMap<>())
      .computeIfAbsent(createIfNull(uri, this::getDefaultURI),
                       u -> new IxIxCacheManager(this,
                                                 u,
                                                 createIfNull(classLoader, this::getDefaultClassLoader),
                                                 createIfNull(properties, this::getDefaultProperties)));
  }

  @Override
  public ClassLoader getDefaultClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  @Override
  public URI getDefaultURI() {
    try {
      return new URI(CACHE_PROVIDER_URI);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Invalid default URI", e);
    }
  }

  @Override
  public Properties getDefaultProperties() {
    return new Properties();
  }

  @Override
  public CacheManager getCacheManager(URI uri, ClassLoader classLoader) {
    return getCacheManager(uri, classLoader, getDefaultProperties());
  }

  @Override
  public CacheManager getCacheManager() {
    return getCacheManager(getDefaultURI(), getDefaultClassLoader(), getDefaultProperties());
  }

  @Override
  public void close() {
    close(getDefaultURI(), getDefaultClassLoader());
  }

  @Override
  public void close(ClassLoader classLoader) {
    close(getDefaultURI(), classLoader);
  }

  @Override
  public void close(URI uri, ClassLoader classLoader) {
    Map<URI, CacheManager> uriCacheManagerMap = cacheManagers.get(classLoader);
    if (uriCacheManagerMap != null) {
      CacheManager cacheManager = uriCacheManagerMap.remove(uri);
      if (cacheManager != null) {
        cacheManager.close();
      }
      if (uriCacheManagerMap.isEmpty()) {
        cacheManagers.remove(classLoader);
      }
    }
  }

  @Override
  public boolean isSupported(OptionalFeature optionalFeature) {
    // No optional features are supported in this simple implementation
    return false;
  }

  private <T> T createIfNull(T uri, Supplier<T> supplier) {
    return uri == null ? supplier.get() : uri;
  }
}
