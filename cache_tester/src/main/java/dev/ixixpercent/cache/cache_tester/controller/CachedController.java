package dev.ixixpercent.cache.cache_tester.controller;

import dev.ixixpercent.cache.cache_tester.service.CachedService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api", path = "/api")
public class CachedController {

  private final CachedService service;

  public CachedController(CachedService service) {
    this.service = service;
  }

  @Cacheable(cacheNames = "endpoint", keyGenerator = "endpointKeyGenerator")
  @GetMapping("/endpoint")
  public CachedResponse endpoint() {
    return service.getResponse();
  }

  @Cacheable(cacheNames = "withSession", keyGenerator = "sessionAwareKeyGenerator")
  @GetMapping("/session")
  public CachedResponse session() {
    return service.getResponse();
  }

  @Cacheable(cacheNames = "attributes1", keyGenerator = "attributesAwareKeyGenerator")
  @GetMapping("/attributes1")
  public CachedResponse attributes1() {
    return service.getResponse();
  }

  @Cacheable(cacheNames = "attributes2", keyGenerator = "attributesAwareKeyGenerator")
  @GetMapping("/attributes2")
  public CachedResponse attributes2() {
    return service.getResponse();
  }
}

