package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
public class InternshipApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private Item testItem;
    private Item testItem2;

    @BeforeEach
    void setUp() {
        testItem = new Item(1L, "Test Name", "Test Desc", "NEW", "test@example.com");
        testItem2 = new Item(2L, "Test Name2", "Test Desc2", "NEW", "test2@example.com");
    }

    @Test
    void testGetAllItems() throws Exception {
        Mockito.when(itemService.findAll()).thenReturn(Arrays.asList(testItem));

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Test Name")));
    }

    @Test
    void testCreateItemSuccess() throws Exception {
        Mockito.when(itemService.save(Mockito.any())).thenReturn(testItem);

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("test@example.com")));
    }

    @Test
    void testGetItemByIdFound() throws Exception {
        Mockito.when(itemService.findById(1L)).thenReturn(Optional.of(testItem));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test Name")));
    }

    @Test
    void testGetItemByIdNotFound() throws Exception {
        Mockito.when(itemService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateItemFound() throws Exception {
        Mockito.when(itemService.findById(1L)).thenReturn(Optional.of(testItem));
        Mockito.when(itemService.save(Mockito.any())).thenReturn(testItem);

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testItem)))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateItemNotFound() throws Exception {
        Mockito.when(itemService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/items/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testItem)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteItem() throws Exception {
        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testProcessItems() throws Exception {
        Mockito.when(itemService.processItemsAsync())
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(testItem, testItem2)));

        mockMvc.perform(get("/api/items/process"))
                .andExpect(status().isOk());

        List<Item> processedItems = itemService.findAll();
        for (Item item : processedItems) {
            assertEquals("PROCESSED", item.getStatus());
        }
    }


    @Test
    void testCreateItemInvalidEmail() throws Exception {
        testItem.setEmail("bad-email");
        String json = objectMapper.writeValueAsString(testItem);
        ResultActions result = mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));


        result.andExpect(status().isBadRequest());
    }
}
