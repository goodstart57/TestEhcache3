package com.frost.testehcache3.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.frost.testehcache3.cache.CustomCacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("userCacheManager")
    private CustomCacheManager<User> userCacheManager;

    @BeforeEach
    void setUp() {
        userService.clearUsers();
        userService.resetCounter();
    }

    @Test
    void shouldGetUser() throws Exception {
        mockMvc.perform(get("/users/u-1"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("u-1"))
            .andExpect(jsonPath("$.displayName").value("user-u-1-1"));

        assertThat(userCacheManager.get("u-1"))
            .contains(new User("u-1", "user-u-1-1"));
    }

    @Test
    void shouldPutUser() throws Exception {
        mockMvc.perform(
            put("/users/u-2")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"displayName":"User Two"}
                    """)
        )
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("u-2"))
            .andExpect(jsonPath("$.displayName").value("User Two"));

        assertThat(userCacheManager.get("u-2"))
            .contains(new User("u-2", "User Two"));
    }

    @Test
    void shouldEvictUser() throws Exception {
        userService.putUser("u-3", new User("u-3", "User Three"));

        mockMvc.perform(delete("/users/u-3"))
            .andExpect(status().isNoContent());

        assertThat(userCacheManager.get("u-3")).isEmpty();

        mockMvc.perform(get("/users/u-3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("u-3"))
            .andExpect(jsonPath("$.displayName").value("user-u-3-1"));
    }
}
