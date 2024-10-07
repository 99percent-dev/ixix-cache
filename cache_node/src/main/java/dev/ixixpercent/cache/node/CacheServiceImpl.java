package dev.ixixpercent.cache.node;

import com.esotericsoftware.kryo.Kryo;
import com.google.protobuf.ByteString;
import dev.ixixpercent.cache.grpc.CacheServiceGrpc;
import dev.ixixpercent.cache.grpc.CacheServiceProto.ClearRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.ClearResponse;
import dev.ixixpercent.cache.grpc.CacheServiceProto.ContainsKeyRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.ContainsKeyResponse;
import dev.ixixpercent.cache.grpc.CacheServiceProto.Entry;
import dev.ixixpercent.cache.grpc.CacheServiceProto.GetAllRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.GetAllResponse;
import dev.ixixpercent.cache.grpc.CacheServiceProto.GetRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.GetResponse;
import dev.ixixpercent.cache.grpc.CacheServiceProto.IsEmptyRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.IsEmptyResponse;
import dev.ixixpercent.cache.grpc.CacheServiceProto.PutAllRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.PutAllResponse;
import dev.ixixpercent.cache.grpc.CacheServiceProto.PutRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.PutResponse;
import dev.ixixpercent.cache.grpc.CacheServiceProto.RemoveRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.RemoveResponse;
import dev.ixixpercent.cache.grpc.CacheServiceProto.SizeRequest;
import dev.ixixpercent.cache.grpc.CacheServiceProto.SizeResponse;
import dev.ixixpercent.cache.node.serialization.KryoPoolUtil;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CacheServiceImpl extends CacheServiceGrpc.CacheServiceImplBase {

  private final Map<String, ConcurrentHashMap<Object, Object>> maps = new ConcurrentHashMap<>();

  private byte[] serialize(Object obj) {
    Kryo kryo = KryoPoolUtil.borrowKryo();
    try (com.esotericsoftware.kryo.io.Output output = new com.esotericsoftware.kryo.io.Output(4096, -1)) {
      kryo.writeClassAndObject(output, obj);
      return output.toBytes();
    } catch (Exception e) {
      throw new RuntimeException("Serialization error", e);
    } finally {
      KryoPoolUtil.releaseKryo(kryo);
    }
  }

  private Object deserialize(byte[] bytes) {
    Kryo kryo = KryoPoolUtil.borrowKryo();
    try (com.esotericsoftware.kryo.io.Input input = new com.esotericsoftware.kryo.io.Input(bytes)) {
      return kryo.readClassAndObject(input);
    } catch (Exception e) {
      throw new RuntimeException("Deserialization error", e);
    } finally {
      KryoPoolUtil.releaseKryo(kryo);
    }
  }

  private ConcurrentHashMap<Object, Object> getMap(String mapName) {
    log.trace("Getting map {}", mapName);
    return maps.computeIfAbsent(mapName, k -> new ConcurrentHashMap<>());
  }

  @Override
  public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
    String mapName = request.getMapName();
    ConcurrentHashMap<Object, Object> map = getMap(mapName);

    Object key = deserialize(request.getKey().toByteArray());
    Object value = map.get(key);

    log.trace("Getting key [{}] and value [{}]", key, value);
    if (value != null) {
      byte[] serializedValue = serialize(value);
      GetResponse response = GetResponse.newBuilder().setValue(ByteString.copyFrom(serializedValue)).build();
      responseObserver.onNext(response);
    } else {
      responseObserver.onNext(null);
    }
    responseObserver.onCompleted();
  }

  @Override
  public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
    String mapName = request.getMapName();
    ConcurrentHashMap<Object, Object> map = getMap(mapName);

    Object key = deserialize(request.getKey().toByteArray());
    Object value = deserialize(request.getValue().toByteArray());

    log.trace("Putting key [{}] and value [{}]", key, value);
    map.put(key, value);

    PutResponse response = PutResponse.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void remove(RemoveRequest request, StreamObserver<RemoveResponse> responseObserver) {
    String mapName = request.getMapName();
    ConcurrentHashMap<Object, Object> map = getMap(mapName);

    Object key = deserialize(request.getKey().toByteArray());
    map.remove(key);

    RemoveResponse response = RemoveResponse.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void containsKey(ContainsKeyRequest request, StreamObserver<ContainsKeyResponse> responseObserver) {
    String mapName = request.getMapName();
    ConcurrentHashMap<Object, Object> map = getMap(mapName);

    Object key = deserialize(request.getKey().toByteArray());
    boolean exists = map.containsKey(key);

    ContainsKeyResponse response = ContainsKeyResponse.newBuilder().setExists(exists).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void size(SizeRequest request, StreamObserver<SizeResponse> responseObserver) {
    String mapName = request.getMapName();
    ConcurrentHashMap<Object, Object> map = getMap(mapName);

    int size = map.size();

    SizeResponse response = SizeResponse.newBuilder().setSize(size).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void isEmpty(IsEmptyRequest request, StreamObserver<IsEmptyResponse> responseObserver) {
    String mapName = request.getMapName();
    ConcurrentHashMap<Object, Object> map = getMap(mapName);

    boolean isEmpty = map.isEmpty();

    IsEmptyResponse response = IsEmptyResponse.newBuilder().setIsEmpty(isEmpty).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void clear(ClearRequest request, StreamObserver<ClearResponse> responseObserver) {
    String mapName = request.getMapName();
    ConcurrentHashMap<Object, Object> map = getMap(mapName);

    map.clear();

    ClearResponse response = ClearResponse.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void putAll(PutAllRequest request, StreamObserver<PutAllResponse> responseObserver) {
    String mapName = request.getMapName();
    ConcurrentHashMap<Object, Object> map = getMap(mapName);

    for (Entry entry : request.getEntriesList()) {
      Object key = deserialize(entry.getKey().toByteArray());
      Object value = deserialize(entry.getValue().toByteArray());
      map.put(key, value);
    }

    PutAllResponse response = PutAllResponse.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getAll(GetAllRequest request, StreamObserver<GetAllResponse> responseObserver) {
    String mapName = request.getMapName();
    ConcurrentHashMap<Object, Object> map = getMap(mapName);

    GetAllResponse.Builder responseBuilder = GetAllResponse.newBuilder();

    for (ByteString keyByteString : request.getKeysList()) {
      byte[] keyBytes = keyByteString.toByteArray();
      Object key = deserialize(keyBytes);
      Object value = map.get(key);
      if (value != null) {
        byte[] serializedKey = serialize(key);
        byte[] serializedValue = serialize(value);
        Entry entry = Entry
          .newBuilder()
          .setKey(ByteString.copyFrom(serializedKey))
          .setValue(ByteString.copyFrom(serializedValue))
          .build();
        responseBuilder.addEntries(entry);
      }
    }

    GetAllResponse response = responseBuilder.build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

}
