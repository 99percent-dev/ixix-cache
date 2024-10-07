package dev.ixixpercent.cache.cache_tester.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

@Component("sessionAwareKeyGenerator")
public class SessionAwareKeyGenerator extends EndpointKeyGenerator {

  @Override
  public Object generate(Object target, Method method, Object... params) {
    return md5Hex(generateRaw(target, method, params));
  }

  protected String generateRaw(Object target, Method method, Object... params) {
    // Get the original endpoint key from the superclass
    String endpointKey = super.generateRaw(target, method, params);

    // Get the session ID
    String sessionId = getSessionId();

    // Combine the endpoint key with the session ID
    return endpointKey + " " + sessionId;
  }


  private String getSessionId() {
    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs != null) {
      HttpSession session = attrs.getRequest().getSession(false);
      if (session != null) {
        return session.getId();
      }
    }
    return "NoSession";
  }
}
