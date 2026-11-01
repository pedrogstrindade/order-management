package com.gabriel.ordermanagement;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
class OpenApiContractTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    private WebApplicationContext context;

    @Test
    void shouldExposeCorrectResponseSchemasForAllEndpoints()
            throws Exception {

        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();

        String json = mockMvc.perform(get("/v3/api-docs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertNotNull(json);

        // Orders
        check(json, "/api/orders", "post", "201", "OrderResponse");
        check(json, "/api/orders", "post", "400", "ApiErrorResponse");
        check(json, "/api/orders", "post", "404", "ApiErrorResponse");

        check(json, "/api/orders", "get", "400", "ApiErrorResponse");

        check(json, "/api/orders/{id}", "get", "404", "ApiErrorResponse");

        check(json, "/api/orders/{id}/status", "patch",
                "400", "ApiErrorResponse");

        check(json, "/api/orders/{id}/status", "patch",
                "404", "ApiErrorResponse");

        check(json, "/api/orders/{id}/status", "patch",
                "409", "ApiErrorResponse");

        // Products
        check(json, "/api/products", "post", "201", "ProductResponse");
        check(json, "/api/products", "post", "400", "ApiErrorResponse");

        check(json, "/api/products", "get", "400", "ApiErrorResponse");

        check(json, "/api/products/{id}", "get", "404", "ApiErrorResponse");

        check(json, "/api/products/{id}", "put", "400", "ApiErrorResponse");
        check(json, "/api/products/{id}", "put", "404", "ApiErrorResponse");

        check(json, "/api/products/{id}", "delete",
                "404", "ApiErrorResponse");
    }

    private static void check(
            String json,
            String path,
            String method,
            String code,
            String schema
    ) {
        String jsonPath = "$.paths['" + path + "']"
                + "." + method
                + ".responses['" + code + "']"
                + ".content['application/json']"
                + ".schema.$ref";

        String actual = JsonPath.read(json, jsonPath);

        assertEquals(
                "#/components/schemas/" + schema,
                actual,
                "Incorrect OpenAPI schema at: " + jsonPath
        );
    }
}