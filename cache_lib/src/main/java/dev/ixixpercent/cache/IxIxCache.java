package dev.ixixpercent.cache;

import dev.ixixpercent.cache.connector.CacheServiceGrpcFactory;
import dev.ixixpercent.cache.store.CacheStoreManager;
import lombok.extern.slf4j.Slf4j;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class IxIxCache<K, V> implements Cache<K, V> {

  private final CacheManager cacheManager;
  private final String cacheName;
  private final Configuration<K, V> configuration;
  private final Map<K, V> store;
  private volatile boolean isClosed = false;

  public IxIxCache(CacheManager cacheManager,
                   CacheServiceGrpcFactory stubFactory,
                   String cacheName,
                   Configuration<K, V> configuration) {
    log.trace("Creating cache {} using manager {}", cacheName, cacheManager);
    this.cacheManager = cacheManager;
    this.cacheName = cacheName;
    this.configuration = configuration;
    store = new CacheStoreManager<>(cacheName, stubFactory, configuration.getKeyType(), configuration.getValueType());
  }

  @Override
  public String getName() {
    return cacheName;
  }

  @Override
  public CacheManager getCacheManager() {
    return cacheManager;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
    if (clazz.isAssignableFrom(configuration.getClass())) {
      return (C) configuration;
    }
    throw new IllegalArgumentException("Configuration class " + clazz + " is not supported");
  }

  @Override
  public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments) throws
                                                                                          EntryProcessorException {
    // TODO
    return null;
  }

  @Override
  public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys,
                                                       EntryProcessor<K, V, T> entryProcessor,
                                                       Object... arguments) {
    //TODO
    return Map.of();
  }

  @Override
  public boolean isClosed() {
    return isClosed;
  }

  private void checkClosed() {
    if (isClosed()) {
      throw new IllegalStateException("Cache is closed");
    }
  }

  @Override
  public void close() {
    isClosed = true;
    store.clear();
  }

  @Override
  public V get(K key) {
    log.trace("Cache [{}] Getting cached value for key [{}]", cacheName, key);
    checkClosed();
    V value = store.get(key);
    log.trace("Cache [{}] Cached value found {}", cacheName, value);
    return value;
  }

  @Override
  public Map<K, V> getAll(Set<? extends K> keys) {
    checkClosed();
    return keys.stream().filter(store::containsKey).collect(Collectors.toMap(key -> key, store::get, (a, b) -> b));
  }

  @Override
  public boolean containsKey(K key) {
    log.trace("Cache [{}] Checking value existence for key [{}]", cacheName, key);
    checkClosed();
    return store.containsKey(key);
  }

  @Override
  public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener) {
    //TODO
  }

  @Override
  public void put(K key, V value) {
    log.trace("Cache [{}] Putting cached value for key [{}], value {}", cacheName, key, value);
    checkClosed();
    store.put(key, value);
  }

  @Override
  public V getAndPut(K key, V value) {
    log.trace("Cache [{}] Getting and putting cached value for key [{}], value {}", cacheName, key, value);
    checkClosed();
    return store.put(key, value);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    //TODO
  }

  @Override
  public boolean putIfAbsent(K key, V value) {
    checkClosed();
    return store.putIfAbsent(key, value) == null;
  }

  @Override
  public boolean remove(K key) {
    log.trace("Cache [{}] Removing cached value for key [{}]", cacheName, key);
    checkClosed();
    return store.remove(key) != null;
  }

  @Override
  public boolean remove(K key, V oldValue) {
    log.trace("Cache [{}] Removing cached value for key [{}] if value {}", cacheName, key, oldValue);
    checkClosed();
    return store.remove(key, oldValue);
  }

  @Override
  public V getAndRemove(K key) {
    log.trace("Cache [{}] Getting and removing cached value for key [{}]", cacheName, key);
    checkClosed();
    return store.remove(key);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    log.trace("Cache [{}] Replacing cached value for key [{}], old value {}, new value{}",
              cacheName,
              key,
              oldValue,
              newValue);
    checkClosed();
    return store.replace(key, oldValue, newValue);
  }

  @Override
  public boolean replace(K key, V value) {
    log.trace("Cache [{}] Replacing cached value for key [{}] with value {}", cacheName, key, value);
    checkClosed();
    return store.replace(key, value) != null;
  }

  @Override
  public V getAndReplace(K key, V value) {
    log.trace("Cache [{}] Getting and cached value for key [{}] with value {}", cacheName, key, value);
    checkClosed();
    return store.computeIfPresent(key, (k, v) -> store.put(key, value));
  }

  @Override
  public void removeAll(Set<? extends K> keys) {
    log.trace("Cache [{}] Removing all cached values for keys {}", cacheName, keys);
    checkClosed();
    for (K key : keys) {
      store.remove(key);
    }
  }

  @Override
  public void removeAll() {
    log.trace("Cache [{}] Removing all cached values for", cacheName);
    checkClosed();
    store.clear();
  }

  @Override
  public void clear() {
    log.trace("Cache [{}] Clearing all cached values for", cacheName);
    checkClosed();
    store.clear();
  }

  @Override
  public <T> T unwrap(Class<T> clazz) {
    if (clazz.isAssignableFrom(getClass())) {
      return clazz.cast(this);
    }
    throw new IllegalArgumentException("Unwrapping to " + clazz + " is not supported");
  }

  @Override
  public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> config) {
    //TODO
    // Not implemented in this simple cache
  }

  @Override
  public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> config) {
    // TODO
    // Not implemented in this simple cache
  }

  @Override
  public Iterator<Entry<K, V>> iterator() {
    checkClosed();
    final Iterator<Map.Entry<K, V>> iterator = store.entrySet().iterator();
    return new Iterator<Entry<K, V>>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Entry<K, V> next() {
        Map.Entry<K, V> entry = iterator.next();
        return new IxIxCacheEntry<>(entry.getKey(), entry.getValue());
      }
    };
  }

}
