/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.springframework.samples.controller;

import com.microsoft.springframework.samples.dao.TodoItemRepository;
import com.microsoft.springframework.samples.model.TodoItem;
import com.microsoft.springframework.samples.service.TodoItemService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.logging.Logger;

//
// If without ComponentScan annotation, Service class is not wired...
//

@Configuration
@EnableRetry
@RestController
@ComponentScan(basePackages = { "com.microsoft.springframework.samples.service"})
public class TodoListController {
    private static Logger logger = Logger.getLogger(TodoListController.class.getName());

    @Autowired
    private TodoItemRepository todoItemRepository;

    @Autowired
    @Qualifier("r4j")
    private TodoItemService todoItemService;

    public TodoListController() {
    }

    @RequestMapping("/home")
    public Map<String, Object> home() {
        logger.info(" ======= /home =======");
        final Map<String, Object> model = new HashMap<String, Object>();
        model.put("id", UUID.randomUUID().toString());
        model.put("content", "home");
        return model;
    }

    /**
     * HTTP GET
     */
    @RequestMapping(value = "/api/todolist/{index}",
            method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> getTodoItem(@PathVariable("index") Integer index) {
        logger.info(" GET ======= /api/todolist/{" + index + "} =======");
        try {
            return new ResponseEntity<TodoItem>(todoItemService.getTodoItem(index), HttpStatus.OK); 
        } catch (Exception e) {
            logger.severe(e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<String>(index + " not found", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * HTTP GET ALL
     */
    @RequestMapping(value = "/api/todolist", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> getAllTodoItems() {
        logger.info(" GET ======= /api/todolist =======");
        try {
            return new ResponseEntity<>(todoItemService.getAllTodoItems(), HttpStatus.OK);
        } catch (Exception e) {
            logger.severe(e.getClass().getName());
            logger.severe(e.toString());
            return new ResponseEntity<>("Nothing found", HttpStatus.NOT_FOUND);
        }
    }    
    

    /**
     * HTTP POST NEW ONE
     */
    @RequestMapping(value = "/api/todolist", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> addNewTodoItem(@RequestBody TodoItem item) {
        logger.info(" POST ======= /api/todolist ======= " + item);
        try {
            todoItemRepository.save(item);
            return new ResponseEntity<String>("Entity created", HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<String>("Entity creation failed", HttpStatus.CONFLICT);
        }
    }

    /**
     * HTTP PUT UPDATE
     */
    @RequestMapping(value = "/api/todolist", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateTodoItem(@RequestBody TodoItem item) {
        logger.info(" PUT ======= /api/todolist ======= " + item);
        try {
            todoItemRepository.save(item);
            return new ResponseEntity<String>("Entity updated", HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<String>("Entity updating failed", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * HTTP DELETE
     */
    @RequestMapping(value = "/api/todolist/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteTodoItem(@PathVariable("id") Integer id) {
        logger.info(" DELETE ======= /api/todolist/{" + id + "} ======= ");
        try {
            todoItemRepository.deleteById(id);
            return new ResponseEntity<String>("Entity deleted", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>("Entity deletion failed", HttpStatus.NOT_FOUND);
        }

    }
}
