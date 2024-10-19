package dev.ixixpercent.cache.store.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import dev.ixixpercent.cache.store.serialization.pooling.KryoPoolUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KryoSerializer {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static byte[] serialize(Object obj) {
        Kryo kryo = KryoPoolUtil.borrowKryo();
        try (Output output = new Output(4096, -1)) {
            kryo.writeClassAndObject(output, mapper.writeValueAsString(obj));
            return output.toBytes();
        } catch (Exception e) {
            throw new RuntimeException("Serialization error", e);
        } finally {
            KryoPoolUtil.releaseKryo(kryo);
        }
    }

    public static <T> T deserialize(byte[] bytes, Class<T> valueType) {
        Kryo kryo = KryoPoolUtil.borrowKryo();
        try (Input input = new Input(bytes)) {
            return mapper.readValue((String) kryo.readClassAndObject(input), valueType);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization error", e);
        } finally {
            KryoPoolUtil.releaseKryo(kryo);
        }
    }
}
