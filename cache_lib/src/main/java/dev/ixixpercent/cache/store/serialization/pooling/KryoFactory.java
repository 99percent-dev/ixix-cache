package dev.ixixpercent.cache.store.serialization.pooling;


import com.esotericsoftware.kryo.Kryo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Kryo instances are not thread safe so we are using a pool of objects.
 */
@Slf4j
public class KryoFactory implements PooledObjectFactory<Kryo> {

  @Override
  public PooledObject<Kryo> makeObject() throws Exception {
    log.trace("Creating Kryo object");
    Kryo kryo = new Kryo();
    // register classes to optimize serialization
    kryo.register(String.class);
    kryo.register(Integer.class);
    // enable references for object graphs
    kryo.setReferences(true);
    // crash early if encountering classed that are not registered
    kryo.setRegistrationRequired(true);
    return new DefaultPooledObject<>(kryo);
  }

  @Override
  public void destroyObject(PooledObject<Kryo> p) throws Exception {
    // No specific destruction needed
  }

  @Override
  public boolean validateObject(PooledObject<Kryo> p) {
    // Kryo instances are always valid
    return true;
  }

  @Override
  public void activateObject(PooledObject<Kryo> p) throws Exception {
    // No activation needed
  }

  @Override
  public void passivateObject(PooledObject<Kryo> p) throws Exception {
    // No passivation needed
  }
}
