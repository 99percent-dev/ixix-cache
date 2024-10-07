package dev.ixixpercent.cache.store;

import dev.ixixpercent.cache.connector.CacheServiceGrpcFactory;
import dev.ixixpercent.cache.store.near.NearCache;
import dev.ixixpercent.cache.store.remote.RemoteCache;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class CacheStoreManager<K, V> implements Map<K, V> {

  private final Map<K, V> nearMap;
  private final Map<K, V> remoteMap;


  public CacheStoreManager(String cacheName,
                           CacheServiceGrpcFactory stubFactory,
                           Class<K> keyType,
                           Class<V> valueType) {
    nearMap = new NearCache<>();
    remoteMap = new RemoteCache<>(cacheName, stubFactory, keyType, valueType);
  }

  @Override
  public int size() {
    // use the size of the near cache
    // the near cache will eventually be synchronized with remote
    return nearMap.size();
  }

  @Override
  public boolean isEmpty() {
    return nearMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return nearMap.containsKey(key) || remoteMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return nearMap.containsValue(value) || remoteMap.containsValue(value);
  }

  @Override
  public V get(Object key) {
    V value = nearMap.get(key);
    log.trace("Value with key {} in near cache {}", key, value);
    if (value == null) {
      value = remoteMap.get(key);
      log.trace("Value with key {} remote cache {}", key, value);
    }
    return value;
  }

  @Override
  public V put(K key, V value) {
    log.trace("Putting value in local and remote cache, key {} value {}", key, value);
    // update the remote cache regardless, we do not care about the return value
    remoteMap.put(key, value);
    // use the operation result from the near cache
    //TODO commented for testing
    //    nearMap.put(key, value);
    return value;
  }

  @Override
  public V remove(Object key) {
    // remove from the remote cache regardless, we do not care about the return value
    remoteMap.remove(key);
    // use the operation result from the near cache
    return nearMap.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    // TODO implement
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    remoteMap.clear();
    nearMap.clear();
  }

  @Override
  public Set<K> keySet() {
    // use a set of keys present in both remote and near caches
    HashSet<K> result = new HashSet<>(nearMap.keySet());
    result.addAll(new HashSet<>(remoteMap.keySet()));
    return result;
  }

  @Override
  public Collection<V> values() {
    // get a merger of keys and get a list of values
    // with the near cache having priority over the remote one
    return keySet().stream().map(this::get).toList();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    // TODO implement
    throw new UnsupportedOperationException();
  }


  @Override
  public boolean equals(Object o) {
    // TODO investigate behaviour
    // delegate to the near cache implementation
    return nearMap.equals(o);
  }

  @Override
  public int hashCode() {
    // TODO investigate behaviour
    // delegate to the near cache implementation
    return nearMap.hashCode();
  }
}
