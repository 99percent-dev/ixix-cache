package dev.ixixpercent.cache;

import javax.cache.Cache;

class IxIxCacheEntry<K, V> implements Cache.Entry<K, V> {

  private final K key;
  private final V value;

  IxIxCacheEntry(K key, V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public K getKey() {
    return key;
  }

  @Override
  public V getValue() {
    return value;
  }

  @Override
  public <T> T unwrap(Class<T> clazz) {
    throw new UnsupportedOperationException("Unwrap is not supported");
  }
}
