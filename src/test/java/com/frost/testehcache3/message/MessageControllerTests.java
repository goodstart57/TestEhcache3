package com.frost.testehcache3.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class MessageControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageService messageService;

    @Autowired
    @Qualifier("messageCacheManager")
    private CustomCacheManager<String> messageCacheManager;

    @BeforeEach
    void setUp() {
        messageService.clearMessages();
        messageService.resetCounter();
    }

    @Test
    void shouldGetMessage() throws Exception {
        mockMvc.perform(get("/messages/item-1"))
            .andExpect(status().isOk())
            .andExpect(content().string("message-item-1-1"));

        assertThat(messageCacheManager.get("item-1")).contains("message-item-1-1");
    }

    @Test
    void shouldPutMessage() throws Exception {
        mockMvc.perform(
            put("/messages/item-2")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"value":"manual-message"}
                    """)
        )
            .andExpect(status().isOk())
            .andExpect(content().string("manual-message"));

        assertThat(messageCacheManager.get("item-2")).contains("manual-message");
    }

    @Test
    void shouldEvictMessage() throws Exception {
        messageService.putMessage("item-3", "manual-message");

        mockMvc.perform(delete("/messages/item-3"))
            .andExpect(status().isNoContent());

        assertThat(messageCacheManager.get("item-3")).isEmpty();

        mockMvc.perform(get("/messages/item-3"))
            .andExpect(status().isOk())
            .andExpect(content().string("message-item-3-1"));
    }
}
