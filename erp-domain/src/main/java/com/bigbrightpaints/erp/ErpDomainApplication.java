package com.bigbrightpaints.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.bigbrightpaints.erp.core.config.EmailProperties;
import com.bigbrightpaints.erp.core.config.GitHubProperties;
import com.bigbrightpaints.erp.core.config.LicensingProperties;
import com.bigbrightpaints.erp.core.security.JwtProperties;

@SpringBootApplication
@EnableRetry
@EnableScheduling
@EnableConfigurationProperties({
  JwtProperties.class,
  EmailProperties.class,
  LicensingProperties.class,
  GitHubProperties.class
})
public class ErpDomainApplication {

  public static void main(String[] args) {
    SpringApplication.run(ErpDomainApplication.class, args);
  }
}
