/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.springframework.samples.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TodoItemTest {

    @Test
    public void testEqualsObject() {
        final TodoItem itemA = new TodoItem();
        final TodoItem itemB1 = new TodoItem("B".hashCode(), "Item B", "Owner of Item B");
        final TodoItem itemB2 = new TodoItem("B".hashCode(), "Item B", "Owner of Item B");
        final Object nonTodoItem = new Object();
        assertTrue(itemA.equals(itemA));
        assertFalse(itemA.equals(null));
        assertFalse(itemA.equals(nonTodoItem));
        assertFalse(itemA.equals(itemB1));
        assertTrue(itemB1.equals(itemB2));
        assertFalse(itemB1.equals(itemA));
    }

}
