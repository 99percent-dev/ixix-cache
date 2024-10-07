package dev.ixixpercent.cache.cache_tester.controller.key_generator;

import dev.ixixpercent.cache.cache_tester.configuration.CacheConfig;
import dev.ixixpercent.cache.cache_tester.configuration.CachesProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

@Component("attributesAwareKeyGenerator")
public class AttributesAwareKeyGenerator extends SessionAwareKeyGenerator {

  private final CachesProperties cachesProperties;

  public AttributesAwareKeyGenerator(CachesProperties cachesProperties) {
    this.cachesProperties = cachesProperties;
  }

  @Override
  public Object generate(Object target, Method method, Object... params) {
    return md5Hex(generateRaw(target, method, params));
  }

  protected String generateRaw(Object target, Method method, Object... params) {
    // Get the base key from the superclass (includes endpoint and session ID)
    String baseKey = super.generateRaw(target, method, params);

    CacheConfig cacheConfig = cachesProperties.getCaches().get(getCacheName(target, method));

    // Get specific request attributes to include in the key
    String requestAttributes = getRequestAttributes(cacheConfig);

    // Combine base key with request attributes
    return baseKey + " Attributes:" + requestAttributes;
  }

  private String getRequestAttributes(CacheConfig cacheConfig) {


    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs != null) {
      HttpServletRequest request = attrs.getRequest();

      // Retrieve parameter values
      Map<String, String[]> parameterMap = request.getParameterMap();

      List<String> parameters = cacheConfig.getParameters();
      String paramsString = parameters != null ?
                            parameters
                              .stream()
                              .filter(parameterMap::containsKey)
                              .map(name -> name + "=" + Arrays.toString(parameterMap.get(name)))
                              .collect(joining(", ")) :
                            "";

      // Retrieve header values
      List<String> headers = cacheConfig.getHeaders();
      String headersString = headers != null ?
                             headers
                               .stream()
                               .filter(name -> request.getHeader(name) != null)
                               .map(name -> name + "=" + request.getHeader(name))
                               .collect(joining(", ")) :
                             "";

      // Combine parameters and headers
      String combinedAttributes = paramsString;
      if (!headersString.isEmpty()) {
        combinedAttributes += (combinedAttributes.isEmpty() ? "" : ", ") + headersString;
      }

      return combinedAttributes;
    }
    return "NoAttributes";
  }


  private String getCacheName(Object target, Method method) {
    // Attempt to find the @Cacheable annotation on the method
    Cacheable cacheable = AnnotationUtils.findAnnotation(method, Cacheable.class);

    // If not found on the method, check the class
    if (cacheable == null) {
      cacheable = AnnotationUtils.findAnnotation(target.getClass(), Cacheable.class);
    }

    // If @Cacheable is present and has cache names defined, return the first one
    if (cacheable != null && cacheable.cacheNames().length > 0) {
      return cacheable.cacheNames()[0];
    }

    // Default cache name if none is found
    return "defaultCache";
  }
}
