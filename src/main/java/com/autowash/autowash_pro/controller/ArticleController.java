package com.autowash.autowash_pro.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.autowash.autowash_pro.entity.Article;
import com.autowash.autowash_pro.service.ArticleService;

@RestController
@RequestMapping("/api/admin/articles")
@CrossOrigin(origins = "http://localhost:5173") // cần sửa laij
public class ArticleController {

    private final ArticleService articleService;

    // Constructor Injection đồng bộ hệ thống
    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    // 1. API LẤY DANH SÁCH BÀI VIẾT (GET)
    // Hỗ trợ cả Tab trạng thái (?status=DRAFT) và ô Tìm kiếm (?search=tieu_de)
    @GetMapping
    public ResponseEntity<List<Article>> getAdminArticles(
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "search", required = false) String search) {

        List<Article> articles = articleService.getAdminArticles(status, search);
        return ResponseEntity.ok(articles);
    }

    // 2. API TẠO MỚI BÀI VIẾT (POST)
    @PostMapping
    public ResponseEntity<?> createArticle(@RequestBody Article articleRequest) {
        try {
            Article newArticle = articleService.createArticle(articleRequest);
            return ResponseEntity.ok(newArticle);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi tạo bài viết mới: " + e.getMessage());
        }
    }

    // 3. API ĐỔI NHANH TRẠNG THÁI (PATCH)
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleArticleStatus(@PathVariable UUID id) {
        try {
            Article updatedArticle = articleService.toggleArticleStatus(id);
            return ResponseEntity.ok(updatedArticle);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi đổi trạng thái bài viết: " + e.getMessage());
        }
    }

    // 4. API CẬP NHẬT/CHỈNH SỬA TOÀN BỘ BÀI VIẾT (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateArticle(@PathVariable UUID id, @RequestBody Article articleDetails) {
        try {
            Article updatedArticle = articleService.updateArticle(id, articleDetails);
            return ResponseEntity.ok(updatedArticle);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi cập nhật bài viết: " + e.getMessage());
        }
    }

    // 5. API XÓA VĨNH VIỄN BÀI VIẾT (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArticle(@PathVariable UUID id) {
        try {
            articleService.deleteArticleById(id);
            return ResponseEntity.ok("Xóa bài viết vĩnh viễn thành công!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi khi xóa bài viết: " + e.getMessage());
        }
    }
}