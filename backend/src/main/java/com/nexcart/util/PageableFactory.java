package com.nexcart.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageableFactory {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageableFactory() { }

    public static Pageable create(int page, int size, String sortBy, String direction,
                                  Set<String> allowedSortFields) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater.");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE + ".");
        }
        if (sortBy == null || !allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException("Unsupported sort field.");
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("direction must be 'asc' or 'desc'.", ex);
        }

        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }
}
