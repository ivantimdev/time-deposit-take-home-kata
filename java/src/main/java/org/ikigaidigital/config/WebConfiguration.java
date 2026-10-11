package org.ikigaidigital.config;

import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class WebConfiguration {

  // ASSUMPTION A20: neither the servlet container nor Spring knows .yaml, so the served contract
  // would go out as application/octet-stream.
  @Bean
  WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> yamlMimeMapping() {
    return factory -> {
      MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
      mappings.add("yaml", "application/yaml");
      factory.setMimeMappings(mappings);
    };
  }
}
