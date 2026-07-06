package com.autowash.autowash_pro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.autowash.autowash_pro.entity.Article;
import com.autowash.autowash_pro.enums.ArticleCategory;
import com.autowash.autowash_pro.enums.ArticleStatus;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

       // 1. Lọc danh sách bài viết theo trạng thái Tab (DRAFT hoặc PUBLISHED)
       List<Article> findByStatus(ArticleStatus status);

       // 2. Tìm kiếm thông minh: Cho phép Admin tìm theo Tiêu đề bài viết HOẶC Tên tác
       // giả
       @Query("SELECT a FROM Article a " +
                     "WHERE LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                     "OR LOWER(a.author) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       List<Article> searchArticles(@Param("keyword") String keyword);

       // 3. Kết hợp nâng cao: Tìm kiếm từ khóa bên trong một Tab trạng thái cụ thể
       @Query("SELECT a FROM Article a " +
                     "WHERE a.status = :status " +
                     "AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                     "OR LOWER(a.author) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       List<Article> searchArticlesByStatus(@Param("status") ArticleStatus status, @Param("keyword") String keyword);

       // 4. Tìm kiếm phía Client (chỉ lấy bài viết đã xuất bản - PUBLISHED)
       List<Article> findByCategoryAndStatus(ArticleCategory category, ArticleStatus status);

       @Query("SELECT a FROM Article a WHERE a.status = com.autowash.autowash_pro.enums.ArticleStatus.PUBLISHED " +
                     "AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                     "OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       List<Article> searchClientArticles(@Param("keyword") String keyword);

       @Query("SELECT a FROM Article a WHERE a.status = com.autowash.autowash_pro.enums.ArticleStatus.PUBLISHED " +
                     "AND a.category = :category " +
                     "AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                     "OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       List<Article> searchClientArticlesByCategory(@Param("category") ArticleCategory category,
                     @Param("keyword") String keyword);

       Optional<Article> findBySlugAndStatus(String slug, ArticleStatus status);

       Optional<Article> findByIdAndStatus(UUID id, ArticleStatus status);
}