package dev.ixixpercent.cache.cache_tester.controller;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

@Component("endpointKeyGenerator")
public class EndpointKeyGenerator implements KeyGenerator {

  @Override
  public Object generate(Object target, Method method, Object... params) {
    return md5Hex(generateRaw(target, method, params));
  }

  protected String generateRaw(Object target, Method method, Object... params) {
    String classMapping = getClassMapping(target.getClass());
    String methodMapping = getMethodMapping(method);
    String httpMethod = getHttpMethod(method);

    return httpMethod + " " + classMapping + methodMapping;
  }

  private String getClassMapping(Class<?> clazz) {
    RequestMapping classRequestMapping = clazz.getAnnotation(RequestMapping.class);
    if (classRequestMapping != null && classRequestMapping.value().length > 0) {
      String value = classRequestMapping.value()[0];
      String path = classRequestMapping.path()[0];
      // value and path can have different values only if one is empty
      // value and path can also have the exact same value (value is alias for path in @RequestMapping)
      return value.equals(path) ? value : path.concat(value);
    }
    return "";
  }

  private String getMethodMapping(Method method) {
    String[] mappings = extractMappings(method);
    if (mappings.length > 0) {
      return mappings[0];
    }
    return "";
  }

  private String getHttpMethod(Method method) {
    if (method.isAnnotationPresent(GetMapping.class)) {
      return "GET";
    } else if (method.isAnnotationPresent(PostMapping.class)) {
      return "POST";
    } else if (method.isAnnotationPresent(PutMapping.class)) {
      return "PUT";
    } else if (method.isAnnotationPresent(DeleteMapping.class)) {
      return "DELETE";
    } else if (method.isAnnotationPresent(PatchMapping.class)) {
      return "PATCH";
    } else if (method.isAnnotationPresent(RequestMapping.class)) {
      RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
      if (requestMapping.method().length > 0) {
        return requestMapping.method()[0].name();
      }
    }
    return "UNKNOWN";
  }

  private String[] extractMappings(Method method) {
    if (method.isAnnotationPresent(GetMapping.class)) {
      return method.getAnnotation(GetMapping.class).value();
    } else if (method.isAnnotationPresent(PostMapping.class)) {
      return method.getAnnotation(PostMapping.class).value();
    } else if (method.isAnnotationPresent(PutMapping.class)) {
      return method.getAnnotation(PutMapping.class).value();
    } else if (method.isAnnotationPresent(DeleteMapping.class)) {
      return method.getAnnotation(DeleteMapping.class).value();
    } else if (method.isAnnotationPresent(PatchMapping.class)) {
      return method.getAnnotation(PatchMapping.class).value();
    } else if (method.isAnnotationPresent(RequestMapping.class)) {
      return method.getAnnotation(RequestMapping.class).value();
    }
    return new String[]{""};
  }
}
