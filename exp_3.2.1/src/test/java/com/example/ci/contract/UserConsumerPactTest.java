package com.example.ci.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer Contract Test:
 * Defines expectations from the consumer perspective ("FrontendConsumer") on "UserService".
 * Running this test generates the contract file: target/pacts/FrontendConsumer-UserService.json
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "UserService", pactVersion = PactSpecVersion.V3)
public class UserConsumerPactTest {

    @Pact(consumer = "FrontendConsumer")
    public RequestResponsePact getUserByIdPact(PactDslWithProvider builder) {
        return builder
                .given("user 1 exists")
                .uponReceiving("A request for user 1")
                .path("/api/users/1")
                .method("GET")
                .headers(Map.of("Accept", "application/json"))
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body("""
                        {
                            "id": 1,
                            "name": "Alice Smith",
                            "email": "alice@example.com",
                            "role": "ADMIN",
                            "active": true
                        }
                        """)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getUserByIdPact")
    @DisplayName("Contract Test (Consumer): Generate Pact contract and verify consumer HTTP interaction")
    void testGetUserContract(MockServer mockServer) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl() + "/api/users/1"))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Alice Smith");
        assertThat(response.body()).contains("alice@example.com");
    }
}
