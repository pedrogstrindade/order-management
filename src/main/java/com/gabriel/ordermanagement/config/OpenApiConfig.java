package com.gabriel.ordermanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Management API")
                        .version("1.0.0")
                        .description("""
                                REST API for managing products and orders.

                                Features:
                                - Product management
                                - Order creation and retrieval
                                - Pagination and sorting
                                - Order status lifecycle
                                - Transactional persistence
                                - Optimistic locking
                                """));
    }
}