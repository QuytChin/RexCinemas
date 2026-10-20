package com.rexchain.cinema.repository;

import com.rexchain.cinema.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Article> {
    List<Article> findByActiveTrueOrderBySortOrderAscPublishDateDesc();
    List<Article> findTop6ByActiveTrueOrderBySortOrderAscPublishDateDesc();
    List<Article> findTop5ByActiveTrueOrderBySortOrderAscPublishDateDesc();
}
