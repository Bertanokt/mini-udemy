package com.miniudemy.course.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.miniudemy.course.dto.CategoryResponse;
import com.miniudemy.course.service.CategoryService;

/** Kategoriler (docs/api/openapi.yaml › Categories). */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** Herkese açık (SecurityConfig › PUBLIC_GET); ada göre Türkçe alfabetik sıra. */
    @GetMapping
    public List<CategoryResponse> listCategories() {
        return categoryService.listCategories();
    }
}
