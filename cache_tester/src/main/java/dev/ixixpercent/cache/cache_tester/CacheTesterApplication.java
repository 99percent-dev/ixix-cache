package dev.ixixpercent.cache.cache_tester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CacheTesterApplication {


  public static void main(String[] args) {
    SpringApplication.run(CacheTesterApplication.class, args);
  }

}
