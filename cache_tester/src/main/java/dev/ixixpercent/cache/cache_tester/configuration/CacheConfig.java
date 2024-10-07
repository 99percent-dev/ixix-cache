package dev.ixixpercent.cache.cache_tester.configuration;

import java.util.List;

public class CacheConfig {
  private List<String> parameters;
  private List<String> headers;
  private long ttl;

  public List<String> getParameters() {
    return this.parameters;
  }

  public List<String> getHeaders() {
    return this.headers;
  }

  public void setParameters(List<String> parameters) {
    this.parameters = parameters;
  }

  public void setHeaders(List<String> headers) {
    this.headers = headers;
  }

  public long getTtl() {
    return ttl;
  }

  public void setTtl(long ttl) {
    this.ttl = ttl;
  }
}