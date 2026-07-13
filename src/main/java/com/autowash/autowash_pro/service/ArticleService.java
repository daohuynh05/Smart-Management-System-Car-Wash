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

    public List<Article> getAdminArticles(String statusStr, String keyword) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatus = statusStr != null && !statusStr.equalsIgnoreCase("ALL");

        if (hasStatus) {
            try {
                ArticleStatus status = ArticleStatus.valueOf(statusStr.toUpperCase());
                if (hasKeyword) {
                    return articleRepository.searchArticlesByStatus(status, keyword.trim());
                }

                return articleRepository.findByStatus(status);
            } catch (IllegalArgumentException e) {
                // gloabl exception handle
            }
        }

        if (hasKeyword) {
            return articleRepository.searchArticles(keyword.trim());
        }

        return articleRepository.findAll();
    }

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

        // Tái tạo lại slug theo tiêu đề mới nếu tiêu đề thay đổi
        String newSlug = details.getTitle().toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");
        article.setSlug(newSlug + "-" + System.currentTimeMillis() % 10000);

        return articleRepository.save(article);
    }

    @Transactional
    public void deleteArticleById(UUID id) {
        if (!articleRepository.existsById(id)) {
            throw new RuntimeException("Không thể xóa! Không tìm thấy bài viết với ID: " + id);
        }
        articleRepository.deleteById(id);
    }

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

    public Article getArticleBySlug(String slug) {
        return articleRepository.findBySlugAndStatus(slug, ArticleStatus.PUBLISHED)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết với slug: " + slug));
    }

    public Article getArticleById(UUID id) {
        return articleRepository.findByIdAndStatus(id, ArticleStatus.PUBLISHED)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết với ID: " + id));
    }
}