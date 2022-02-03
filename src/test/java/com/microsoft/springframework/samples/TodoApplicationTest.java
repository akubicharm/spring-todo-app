/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.springframework.samples;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.microsoft.springframework.samples.controller.TodoListController;
import com.microsoft.springframework.samples.dao.TodoItemRepository;
import com.microsoft.springframework.samples.model.TodoItem;

import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.json.JSONArray;
import org.json.JSONObject;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:test.properties")
@WebMvcTest(TodoListController.class)
public class TodoApplicationTest {
    static final String MOCK_ID = "mockId";
    static final String MOCK_DESC = "Mock Item";
    static final String MOCK_OWNER = "Title of mock item";
    final Map<Integer, TodoItem> repository = new HashMap<>();
    final TodoItem mockItemA = new TodoItem(8 /* (MOCK_ID + "-A").hashCode() */, MOCK_DESC + "-A", MOCK_OWNER + "-A");
    final TodoItem mockItemB = new TodoItem((MOCK_ID + "-B").hashCode(), MOCK_DESC + "-B", MOCK_OWNER + "-B");

    final TodoItem[] mockItems = new TodoItem[] {mockItemA, mockItemB};

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoItemRepository todoItemRepository;

    @BeforeEach
    public void setUp() {
        repository.clear();
        repository.put(mockItemA.getId(), mockItemA);
        repository.put(mockItemB.getId(), mockItemB);      

        given(this.todoItemRepository.save(any(TodoItem.class))).willAnswer((InvocationOnMock invocation) -> {
            final TodoItem item = invocation.getArgument(0);
            if (repository.containsKey(item.getId())) {
                throw new Exception("Conflict.");
            }
            repository.put(item.getId(), item);
            return item;
        });

        given(this.todoItemRepository.findById(any(Integer.class))).willAnswer((InvocationOnMock invocation) -> {
            final Integer id = invocation.getArgument(0);
            if (id == 8) {
                throw new org.postgresql.util.PSQLException("DUMMY", org.postgresql.util.PSQLState.CONNECTION_FAILURE);
            }
            return Optional.of(repository.get(id));
        });

        given(this.todoItemRepository.findAll()).willAnswer((InvocationOnMock invocation) -> {
            return new ArrayList<TodoItem>(repository.values());
        });

        willAnswer((InvocationOnMock invocation) -> {
            final Integer id = invocation.getArgument(0);
            if (!repository.containsKey(id)) {
                throw new Exception("Not Found.");
            }
            repository.remove(id);
            return null;
        }).given(this.todoItemRepository).deleteById(any(Integer.class));
    }

    @After
    public void tearDown() {
        repository.clear();
    }

    @Test
    public void shouldRenderDefaultTemplate() throws Exception {
        mockMvc.perform(get("/")).andDo(print()).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
    }

    @Test
    public void canGetTodoItem() throws Exception {
        JSONObject jo = new JSONObject().put("id", mockItemA.getId()).put("title", mockItemA.getTitle()).put("description", mockItemA.getDescription());
        mockMvc.perform(get(String.format("/api/todolist/%s", mockItemA.getId().toString()))).andDo(print())
                .andExpect(status().isNotFound());
        
        jo = new JSONObject().put("id", mockItemB.getId()).put("title", mockItemB.getTitle()).put("description", mockItemB.getDescription());
        mockMvc.perform(get(String.format("/api/todolist/%s", mockItemB.getId().toString()))).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(jo.toString()));

    }

    @Test
    public void canGetAllTodoItems() throws Exception {
        JSONArray jArray = new JSONArray();
        for (int i = 0; i < mockItems.length; i++) {
            TodoItem ti = mockItems[i];
            jArray.put(new JSONObject().put("id", ti.getId()).put("title", ti.getTitle()).put("description", ti.getDescription()));
        }
        mockMvc.perform(get("/api/todolist")).andDo(print()).andExpect(status().isOk()).andExpect(content().json(jArray.toString()));
    }

    //@Test
    public void canSaveTodoItems() throws Exception {
        final int size = repository.size();
        final TodoItem mockItemC = new TodoItem(null, MOCK_DESC + "-C", MOCK_OWNER + "-C");
        mockMvc.perform(post("/api/todolist").contentType(MediaType.APPLICATION_JSON_VALUE).content(String
                .format("{\"description\":\"%s\",\"title\":\"%s\"}", mockItemC.getDescription(), mockItemC.getTitle())))
                .andDo(print()).andExpect(status().isCreated());
        assertTrue(size + 1 == repository.size());
    }

    //@Test
    public void canDeleteTodoItems() throws Exception {
        final int size = repository.size();
        mockMvc.perform(delete(String.format("/api/todolist/%s", mockItemA.getId()))).andDo(print())
                .andExpect(status().isOk());
        assertTrue(size - 1 == repository.size());
        assertFalse(repository.containsKey(mockItemA.getId()));
    }

    //@Test
    public void canUpdateTodoItems() throws Exception {
        final String newItemJsonString = String.format("{\"id\":\"%s\",\"description\":\"%s\",\"title\":\"%s\"}",
                mockItemA.getId(), mockItemA.getDescription(), "New Title");
        mockMvc.perform(put("/api/todolist").contentType(MediaType.APPLICATION_JSON_VALUE).content(newItemJsonString))
                .andDo(print()).andExpect(status().isOk());
        assertTrue(repository.get(mockItemA.getId()).getTitle().equals("New Title"));
    }

    //@Test
    public void canNotDeleteNonExistingTodoItems() throws Exception {
        final int size = repository.size();
        mockMvc.perform(delete(String.format("/api/todolist/%s", "Non-Existing-ID"))).andDo(print())
                .andExpect(status().isNotFound());
        assertTrue(size == repository.size());
    }
}
