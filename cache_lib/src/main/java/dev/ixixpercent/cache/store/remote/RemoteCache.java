package dev.ixixpercent.cache.store.remote;

import com.esotericsoftware.kryo.Kryo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.google.protobuf.ByteString;
import dev.ixixpercent.cache.connector.CacheServiceGrpcFactory;
import dev.ixixpercent.cache.grpc.CacheServiceGrpc;
import dev.ixixpercent.cache.grpc.CacheServiceProto.ClearRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.ContainsKeyRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.GetAllRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.GetAllResponse;
import dev.ixixpercent.cache.grpc.CacheServiceProto.GetRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.GetResponse;
import dev.ixixpercent.cache.grpc.CacheServiceProto.IsEmptyRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.PutAllRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.PutRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.RemoveRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.SizeRequest;
import dev.ixixpercent.cache.store.serialization.pooling.KryoPoolUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
public class RemoteCache<K, V> implements Map<K, V> {

  private final ObjectMapper mapper;
  private final String mapName;
  private final CacheServiceGrpcFactory stubFactory;
  // TODO key type is currently just string but if other classes are used the serialization will fail
  private final Class<K> keyType;
  private final Class<V> valueType;
  private final CacheServiceGrpc.CacheServiceBlockingStub stub;


  public RemoteCache(String mapName, CacheServiceGrpcFactory stubFactory, Class<K> keyType, Class<V> valueType) {
    this.mapName = mapName;
    this.stubFactory = stubFactory;
    this.keyType = keyType;
    this.valueType = valueType;
    mapper = new ObjectMapper();
    this.mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                                      ObjectMapper.DefaultTyping.EVERYTHING,
                                      JsonTypeInfo.As.PROPERTY);
    this.stub = stubFactory.getStub();
  }


  // Kryo Serialization
  private byte[] serialize(Object obj) {
    Kryo kryo = KryoPoolUtil.borrowKryo();
    try (com.esotericsoftware.kryo.io.Output output = new com.esotericsoftware.kryo.io.Output(4096, -1)) {
      kryo.writeClassAndObject(output, mapper.writeValueAsString(obj));
      return output.toBytes();
    } catch (Exception e) {
      throw new RuntimeException("Serialization error", e);
    } finally {
      KryoPoolUtil.releaseKryo(kryo);
    }
  }

  // Kryo Deserialization
  private Object deserialize(byte[] bytes) {
    Kryo kryo = KryoPoolUtil.borrowKryo();
    try (com.esotericsoftware.kryo.io.Input input = new com.esotericsoftware.kryo.io.Input(bytes)) {
      return mapper.readValue((String) kryo.readClassAndObject(input), valueType);
    } catch (Exception e) {
      throw new RuntimeException("Deserialization error", e);
    } finally {
      KryoPoolUtil.releaseKryo(kryo);
    }
  }

  @Override
  public int size() {
    return stub.size(SizeRequest.newBuilder().setMapName(mapName).build()).getSize();
  }

  @Override
  public boolean isEmpty() {
    return stub.isEmpty(IsEmptyRequest.newBuilder().setMapName(mapName).build()).getIsEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return stub
      .containsKey(ContainsKeyRequest
                     .newBuilder()
                     .setKey(ByteString.copyFrom(serialize(key)))
                     .setMapName(mapName)
                     .build())
      .getExists();
  }

  @Override
  public boolean containsValue(Object value) {
    // TODO Not efficient: Fetch all values and check. Alternatively, implement on server side.
    return values().contains(value);
  }

  @Override
  public V get(Object key) {
    GetResponse response =
      stub.get(GetRequest.newBuilder().setKey(ByteString.copyFrom(serialize(key))).setMapName(mapName).build());
    if (response.getValue().isEmpty()) {
      return null;
    }
    return (V) deserialize(response.getValue().toByteArray());
  }

  @Override
  public V put(K key, V value) {
    stub.put(PutRequest
               .newBuilder()
               .setKey(ByteString.copyFrom(serialize(key)))
               .setValue(ByteString.copyFrom(serialize(value)))
               .setMapName(mapName)
               .build());
    // TODO implement to respect Map semantics
    return value;
  }

  @Override
  public V remove(Object key) {
    RemoveRequest request =
      RemoveRequest.newBuilder().setKey(ByteString.copyFrom(serialize(key))).setMapName(mapName).build();
    stub.remove(request);
    // TODO implement to respect Map semantics
    return null;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    PutAllRequest.Builder requestBuilder = PutAllRequest.newBuilder();
    for (Entry<? extends K, ? extends V> entry : map.entrySet()) {

      var protoEntry = dev.ixixpercent.cache.grpc.CacheServiceProto.Entry
        .newBuilder()
        .setKey(ByteString.copyFrom(serialize(entry.getKey())))
        .setValue(ByteString.copyFrom(serialize(entry.getValue())))
        .build();
      requestBuilder.addEntries(protoEntry);
    }
    stub.putAll(requestBuilder.setMapName(mapName).build());
  }

  @Override
  public void clear() {
    stub.clear(ClearRequest.newBuilder().setMapName(mapName).build());
  }

  @Override
  public Set<K> keySet() {
    GetAllRequest request = GetAllRequest.newBuilder().setMapName(mapName).build();
    GetAllResponse response = stub.getAll(request);
    Set<K> keys = new HashSet<>();
    for (dev.ixixpercent.cache.grpc.CacheServiceProto.Entry entry : response.getEntriesList()) {
      keys.add((K) deserialize(entry.getKey().toByteArray()));
    }
    return keys;
  }

  @Override
  public Collection<V> values() {
    GetAllRequest request = GetAllRequest.newBuilder().setMapName(mapName).build();
    GetAllResponse response = stub.getAll(request);
    List<V> values = new ArrayList<>();
    for (dev.ixixpercent.cache.grpc.CacheServiceProto.Entry entry : response.getEntriesList()) {
      values.add((V) deserialize(entry.getValue().toByteArray()));
    }
    return values;
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    GetAllRequest request = GetAllRequest.newBuilder().setMapName(mapName).build();
    GetAllResponse response = stub.getAll(request);
    Set<Entry<K, V>> entries = new HashSet<>();
    for (dev.ixixpercent.cache.grpc.CacheServiceProto.Entry entry : response.getEntriesList()) {
      K key = (K) deserialize(entry.getKey().toByteArray());
      V value = (V) deserialize(entry.getValue().toByteArray());
      entries.add(new AbstractMap.SimpleEntry<>(key, value));
    }
    return entries;
  }

  @Override
  public V getOrDefault(Object key, V defaultValue) {
    V value = get(key);
    return (value != null) ? value : defaultValue;
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    Objects.requireNonNull(action);
    for (Entry<K, V> entry : entrySet()) {
      action.accept(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    Objects.requireNonNull(function);
    for (Entry<K, V> entry : entrySet()) {
      V newValue = function.apply(entry.getKey(), entry.getValue());
      put(entry.getKey(), newValue);
    }
  }

  @Override
  public V putIfAbsent(K key, V value) {
    if (!containsKey(key)) {
      put(key, value);
      return null;
    }
    return get(key);
  }

  @Override
  public boolean remove(Object key, Object value) {
    if (containsKey(key) && Objects.equals(get(key), value)) {
      remove(key);
      return true;
    }
    return false;
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    if (containsKey(key) && Objects.equals(get(key), oldValue)) {
      put(key, newValue);
      return true;
    }
    return false;
  }

  @Override
  public V replace(K key, V value) {
    if (containsKey(key)) {
      put(key, value);
      return value;
    }
    return null;
  }

  @Override
  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    Objects.requireNonNull(mappingFunction);
    V value = get(key);
    if (value == null) {
      V newValue = mappingFunction.apply(key);
      if (newValue != null) {
        put(key, newValue);
        return newValue;
      }
    }
    return value;
  }

  @Override
  public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    V oldValue = get(key);
    if (oldValue != null) {
      V newValue = remappingFunction.apply(key, oldValue);
      if (newValue != null) {
        put(key, newValue);
        return newValue;
      } else {
        remove(key);
        return null;
      }
    }
    return null;
  }

  @Override
  public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    V oldValue = get(key);
    V newValue = remappingFunction.apply(key, oldValue);
    if (newValue == null) {
      remove(key);
      return null;
    } else {
      put(key, newValue);
      return newValue;
    }
  }

  @Override
  public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    Objects.requireNonNull(value);
    V oldValue = get(key);
    V newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);
    if (newValue == null) {
      remove(key);
      return null;
    } else {
      put(key, newValue);
      return newValue;
    }
  }

}
