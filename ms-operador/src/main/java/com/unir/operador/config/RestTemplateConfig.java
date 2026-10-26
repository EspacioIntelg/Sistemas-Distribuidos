package com.unir.operador.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced // Permite usar el nombre de Eureka (ms-buscador) en vez de IP:puerto.
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
