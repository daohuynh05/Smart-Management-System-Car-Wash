package com.autowash.autowash_pro.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.autowash.autowash_pro.entity.Article;
import com.autowash.autowash_pro.enums.ArticleCategory;
import com.autowash.autowash_pro.enums.ArticleStatus;
import com.autowash.autowash_pro.repository.ArticleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    // 1. LUỒNG LẤY DANH SÁCH: Kết hợp thông minh giữa gõ ô Tìm kiếm và bộ lọc Tab Trạng thái
    public List<Article> getAdminArticles(String statusStr, String keyword) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatus = statusStr != null && !statusStr.equalsIgnoreCase("ALL");

        // Trường hợp 1: Có bộ lọc Tab (DRAFT/PUBLISHED)
        if (hasStatus) {
            try {
                ArticleStatus status = ArticleStatus.valueOf(statusStr.toUpperCase());
                if (hasKeyword) {
                    // Lọc từ khóa bên trong Tab đó
                    return articleRepository.searchArticlesByStatus(status, keyword.trim());
                }
                // Chỉ lọc theo Tab
                return articleRepository.findByStatus(status);
            } catch (IllegalArgumentException e) {
                // Nếu chuỗi status sai định dạng, tự động rơi xuống bộ lọc chung bên dưới
            }
        }

        // Trường hợp 2: Tab "Tất cả" nhưng Admin có gõ ô Tìm kiếm
        if (hasKeyword) {
            return articleRepository.searchArticles(keyword.trim());
        }

        // Trường hợp 3: Mặc định lôi toàn bộ bài viết từ Supabase lên
        return articleRepository.findAll();
    }

    // 2. LUỒNG TẠO MỚI BÀI VIẾT ĐỘNG
    @Transactional
    public Article createArticle(Article articleRequest) {
        // Tự động tạo slug đơn giản từ tiêu đề nếu phía Frontend chưa kịp truyền lên
        if (articleRequest.getSlug() == null || articleRequest.getSlug().trim().isEmpty()) {
            String slugified = articleRequest.getTitle().toLowerCase()
                    .replaceAll("[^a-z0-9\\s]", "")
                    .replaceAll("\\s+", "-");
            articleRequest.setSlug(slugified + "-" + System.currentTimeMillis() % 10000);
        }
        return articleRepository.save(articleRequest);
    }

    // 3. LUỒNG ĐỔI TRẠNG THÁI NHANH (DRAFT <-> PUBLISHED)
    @Transactional
    public Article toggleArticleStatus(UUID id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết với ID: " + id));

        if (article.getStatus() == ArticleStatus.PUBLISHED) {
            article.setStatus(ArticleStatus.DRAFT);
        } else {
            article.setStatus(ArticleStatus.PUBLISHED);
        }

        return articleRepository.save(article);
    }

    // 4. LUỒNG CẬP NHẬT/CHỈNH SỬA CHI TIẾT BÀI VIẾT
    @Transactional
    public Article updateArticle(UUID id, Article details) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết với ID: " + id));
        
        // Cập nhật từng trường dữ liệu động từ Frontend gửi sang
        article.setTitle(details.getTitle());
        article.setSummary(details.getSummary());
        article.setContent(details.getContent());
        article.setCoverImage(details.getCoverImage());
        article.setCategory(details.getCategory());
        article.setStatus(details.getStatus());
        article.setAuthor(details.getAuthor());
        
        // Tái tạo lại slug theo tiêu đề mới để tối ưu SEO URL nếu tiêu đề thay đổi
        String newSlug = details.getTitle().toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");
        article.setSlug(newSlug + "-" + System.currentTimeMillis() % 10000);

        return articleRepository.save(article);
    }

    // 5. LUỒNG XÓA BÀI VIẾT KHỎI DATABASE
    @Transactional
    public void deleteArticleById(UUID id) {
        if (!articleRepository.existsById(id)) {
            throw new RuntimeException("Không thể xóa! Không tìm thấy bài viết với ID: " + id);
        }
        articleRepository.deleteById(id);
    }

    // 6. LUỒNG LẤY DANH SÁCH BÀI VIẾT PHÍA CLIENT
    public List<Article> getClientArticles(String categoryStr, String search) {
        ArticleCategory category = null;
        if (categoryStr != null && !categoryStr.trim().isEmpty() && !categoryStr.equalsIgnoreCase("ALL")) {
            try {
                category = ArticleCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Bỏ qua category không hợp lệ
            }
        }
        
        boolean hasKeyword = search != null && !search.trim().isEmpty();
        String keyword = hasKeyword ? search.trim() : null;

        if (category != null) {
            if (hasKeyword) {
                return articleRepository.searchClientArticlesByCategory(category, keyword);
            } else {
                return articleRepository.findByCategoryAndStatus(category, ArticleStatus.PUBLISHED);
            }
        } else {
            if (hasKeyword) {
                return articleRepository.searchClientArticles(keyword);
            } else {
                return articleRepository.findByStatus(ArticleStatus.PUBLISHED);
            }
        }
    }

    // 7. LUỒNG LẤY CHI TIẾT BÀI VIẾT THEO SLUG
    public Article getArticleBySlug(String slug) {
        return articleRepository.findBySlugAndStatus(slug, ArticleStatus.PUBLISHED)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết với slug: " + slug));
    }

    // 8. LUỒNG LẤY CHI TIẾT BÀI VIẾT THEO ID
    public Article getArticleById(UUID id) {
        return articleRepository.findByIdAndStatus(id, ArticleStatus.PUBLISHED)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết với ID: " + id));
    }
}