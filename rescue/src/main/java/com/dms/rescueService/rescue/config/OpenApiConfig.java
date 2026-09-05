package com.dms.rescueService.rescue.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(
            @Value("${spring.application.name:Service}") String serviceName) {
        return new OpenAPI()
                .info(new Info()
                        .title(serviceName.toUpperCase() + " API")
                        .description("REST API specifications for " + serviceName + " in the Disaster Management Platform.")
                        .version("v1.0")
                        .contact(new Contact().name("Disaster Management Core Team")));
    }
}