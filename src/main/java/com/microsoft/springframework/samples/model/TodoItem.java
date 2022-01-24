/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.springframework.samples.model;

import lombok.Data;

import org.springframework.data.annotation.Id;

import java.util.Objects;


@Data
public class TodoItem {
    @Id
    private Integer id;
    private String description;
    private String title;
    private boolean finished;

    public TodoItem() {
    }

    public TodoItem(String title, String description) {
        this(null, title, description);
    }

    public TodoItem(Integer id, String title, String description) {
        this.description = description;
        this.id = id;
        this.title = title;
        this.finished = false;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinish(final boolean finished) {
        this.finished = finished;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TodoItem)) {
            return false;
        }
        final TodoItem group = (TodoItem) o;
        return Objects.equals(this.getDescription(), group.getDescription())
                && Objects.equals(this.getTitle(), group.getTitle())
                && Objects.equals(this.getId(), group.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, id, title);
    }

    @Override
    public String toString() {
        if (id != null) {
            return id + ": " + title + ": " + description;
        } else {
            return title + ": " + description;
        }
    }
}

