package dev.ixixpercent.cache.cache_tester.service;

import dev.ixixpercent.cache.cache_tester.controller.CachedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CachedService {

  public CachedResponse getResponse() {
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Thread was interrupted");
    }
    log.info("Returning response after delay (not cached)");
    return new CachedResponse("Hello, World!", System.currentTimeMillis());
  }
}
