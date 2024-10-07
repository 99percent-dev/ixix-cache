package dev.ixixpercent.cache.store.serialization.pooling;

import com.esotericsoftware.kryo.Kryo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

@Slf4j
public class KryoPoolUtil {
  private static final GenericObjectPool<Kryo> pool;

  static {
    KryoFactory factory = new KryoFactory();
    GenericObjectPoolConfig<Kryo> config = new GenericObjectPoolConfig<>();
    // Maximum number of Kryo instances
    config.setMaxTotal(50);
    // Maximum idle instances
    config.setMaxIdle(20);
    // Minimum idle instances
    config.setMinIdle(5);
    // Validate instances when borrowing
    config.setTestOnBorrow(true);
    config.setTestOnReturn(true);
    // Validate instances when returning
    pool = new GenericObjectPool<>(factory, config);
    log.info("Kryo pool initialized {}", pool);
  }

  public static Kryo borrowKryo() {
    try {
      log.trace("Borrowing Kryo object");
      return pool.borrowObject();
    } catch (Exception e) {
      throw new RuntimeException("Could not borrow Kryo instance from pool", e);
    }
  }

  public static void releaseKryo(Kryo kryo) {
    log.trace("Releasing Kryo object {}", kryo);
    if (kryo != null) {
      pool.returnObject(kryo);
    }
  }
}
