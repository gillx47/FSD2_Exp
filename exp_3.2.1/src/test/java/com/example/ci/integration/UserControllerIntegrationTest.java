package com.example.ci.integration;

import com.example.ci.dto.UserDto;
import com.example.ci.model.User;
import com.example.ci.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Testing Tier:
 * Validates REST API contracts, HTTP status codes, routing, serialization,
 * and end-to-end database interaction using MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration Test: POST /api/users should create and return 201 Created")
    void testCreateUser_ApiIntegration() throws Exception {
        UserDto newUser = new UserDto(null, "Charlie Brown", "charlie@example.com", "DEVELOPER", true);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Charlie Brown")))
                .andExpect(jsonPath("$.email", is("charlie@example.com")))
                .andExpect(jsonPath("$.role", is("DEVELOPER")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    @DisplayName("Integration Test: GET /api/users/{id} should return 200 OK with user JSON")
    void testGetUserById_ApiIntegration() throws Exception {
        User saved = userRepository.save(new User("Diana Prince", "diana@example.com", "ARCHITECT", true));

        mockMvc.perform(get("/api/users/" + saved.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Diana Prince")))
                .andExpect(jsonPath("$.email", is("diana@example.com")));
    }

    @Test
    @DisplayName("Integration Test: GET /api/users/{id} with non-existent ID should return 404 Not Found")
    void testGetUserById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/users/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Integration Test: POST /api/users with blank email should trigger validation and return 400 Bad Request")
    void testCreateUser_InvalidEmail_Returns400() throws Exception {
        UserDto invalidUser = new UserDto(null, "Invalid Guy", "", "USER", true);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());
    }
}
