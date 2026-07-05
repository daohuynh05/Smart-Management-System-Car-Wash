package com.autowash.autowash_pro.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.autowash.autowash_pro.entity.Article;
import com.autowash.autowash_pro.service.ArticleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Tag(name = "Article Client", description = "Xem danh sách bài viết cẩm nang dành cho Khách hàng")
public class ClientArticleController {

    private final ArticleService articleService;

    // 1. API LẤY DANH SÁCH BÀI VIẾT ĐÃ XUẤT BẢN (PUBLISHED)
    @GetMapping
    @Operation(summary = "Lấy danh sách cẩm nang đã xuất bản (PUBLISHED)")
    public ResponseEntity<List<Article>> getClientArticles(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search) {
        
        List<Article> articles = articleService.getClientArticles(category, search);
        return ResponseEntity.ok(articles);
    }

    // 2. API LẤY CHI TIẾT BÀI VIẾT THEO SLUG HOẶC ID
    @GetMapping("/{slug}")
    @Operation(summary = "Lấy chi tiết cẩm nang theo Slug hoặc ID")
    public ResponseEntity<?> getArticleBySlug(@PathVariable String slug) {
        try {
            // Kiểm tra nếu slug thực chất là UUID ID
            try {
                UUID id = UUID.fromString(slug);
                Article article = articleService.getArticleById(id);
                return ResponseEntity.ok(article);
            } catch (IllegalArgumentException e) {
                // Không phải UUID, tìm theo slug thông thường
            }

            Article article = articleService.getArticleBySlug(slug);
            return ResponseEntity.ok(article);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 3. API LẤY CHI TIẾT BÀI VIẾT THEO ID
    @GetMapping("/detail/{id}")
    @Operation(summary = "Lấy chi tiết cẩm nang theo ID")
    public ResponseEntity<?> getArticleById(@PathVariable UUID id) {
        try {
            Article article = articleService.getArticleById(id);
            return ResponseEntity.ok(article);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
