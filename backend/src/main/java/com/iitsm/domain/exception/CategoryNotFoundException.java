package com.iitsm.domain.exception;

public class CategoryNotFoundException extends IitsmException {

    public CategoryNotFoundException(String message) {
        super(message, "CAT_001", 404);
    }

    public CategoryNotFoundException(Long id) {
        super("Category not found with id: " + id, "CAT_001", 404);
    }
}
