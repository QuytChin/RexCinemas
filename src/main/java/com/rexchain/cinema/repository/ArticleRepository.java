package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByActiveTrueOrderBySortOrderAscPublishDateDesc();
}
