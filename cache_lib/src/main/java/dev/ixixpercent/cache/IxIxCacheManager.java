package dev.ixixpercent.cache;

import dev.ixixpercent.cache.connector.CacheServiceGrpcFactory;
import lombok.extern.slf4j.Slf4j;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class IxIxCacheManager implements CacheManager {

  private final CachingProvider cachingProvider;
  private final URI uri;
  private final ClassLoader classLoader;
  private final Properties properties;
  private volatile boolean isClosed = false;
  private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
  private final CacheServiceGrpcFactory stubFactory;

  public IxIxCacheManager(CachingProvider cachingProvider, URI uri, ClassLoader classLoader, Properties properties) {
    this.cachingProvider = cachingProvider;
    this.uri = uri;
    this.classLoader = classLoader;
    this.properties = properties;
    stubFactory = new CacheServiceGrpcFactory();

    stubFactory.addNode("localhost", 50051);
    stubFactory.addNode("localhost", 50052);
    stubFactory.addNode("localhost", 50053);

    log.trace("Created IxIxCacheManager with uri {} and provider {}", uri, cachingProvider);
  }

  @Override
  public CachingProvider getCachingProvider() {
    return cachingProvider;
  }

  @Override
  public URI getURI() {
    return uri;
  }

  @Override
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  @Override
  public Properties getProperties() {
    return properties;
  }

  @Override
  public <K, V, C extends Configuration<K, V>> Cache<K, V> createCache(String cacheName, C configuration) throws
                                                                                                          IllegalArgumentException {
    if (isClosed()) {
      throw new IllegalStateException("CacheManager is closed");
    }
    if (caches.containsKey(cacheName)) {
      throw new CacheException("Cache with name " + cacheName + " already exists");
    }
    Cache<K, V> cache = new IxIxCache<>(this, stubFactory, cacheName, configuration);
    caches.put(cacheName, cache);
    return cache;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
    if (isClosed()) {
      throw new IllegalStateException("CacheManager is closed");
    }
    Cache<?, ?> cache = caches.get(cacheName);
    if (cache == null) {
      return createCache(cacheName, new MutableConfiguration<>());
    }
    Configuration<?, ?> configuration = cache.getConfiguration(Configuration.class);
    if (configuration.getKeyType() != null && !configuration.getKeyType().equals(keyType)) {
      throw new ClassCastException("Incompatible key type");
    }
    if (configuration.getValueType() != null && !configuration.getValueType().equals(valueType)) {
      throw new ClassCastException("Incompatible value type");
    }
    return (Cache<K, V>) cache;
  }

  @Override
  public Cache<?, ?> getCache(String cacheName) {
    if (isClosed()) {
      throw new IllegalStateException("CacheManager is closed");
    }

    Cache<?, ?> cache = caches.get(cacheName);
    return cache == null ? createCache(cacheName, new MutableConfiguration<>()) : cache;
  }

  @Override
  public Iterable<String> getCacheNames() {
    if (isClosed()) {
      throw new IllegalStateException("CacheManager is closed");
    }
    return Collections.unmodifiableSet(caches.keySet());
  }

  @Override
  public void destroyCache(String cacheName) {
    if (isClosed()) {
      throw new IllegalStateException("CacheManager is closed");
    }
    Cache<?, ?> cache = caches.remove(cacheName);
    if (cache != null) {
      cache.close();
    }
  }

  @Override
  public void enableManagement(String cacheName, boolean enabled) {
    // Not implemented in this simple cache manager
  }

  @Override
  public void enableStatistics(String cacheName, boolean enabled) {
    // Not implemented in this simple cache manager
  }

  @Override
  public void close() {
    if (!isClosed) {
      for (Cache<?, ?> cache : caches.values()) {
        cache.close();
      }
      caches.clear();
      isClosed = true;
    }
  }

  @Override
  public boolean isClosed() {
    return isClosed;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T unwrap(Class<T> clazz) {
    if (clazz.isAssignableFrom(getClass())) {
      return (T) this;
    }
    throw new IllegalArgumentException("Unwrapping to " + clazz + " is not supported");
  }
}
