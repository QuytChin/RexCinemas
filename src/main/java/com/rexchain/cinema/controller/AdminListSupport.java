package com.rexchain.cinema.controller;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.FetchParent;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AdminListSupport {
    public record Options(String search, String filter, String[] sorts, String[] fetches,
                          String filterLabel, String searchPlaceholder) {
        public Options(String search, String filter, String[] sorts, String[] fetches) {
            this(search, filter, sorts, fetches, "Trạng thái", "Nhập từ khóa cần tìm");
        }
        public Options(String search, String filter, String... sorts) {
            this(search, filter, sorts, new String[0]);
        }
        public Options withLabels(String filterLabel, String searchPlaceholder) {
            return new Options(search, filter, sorts, fetches, filterLabel, searchPlaceholder);
        }
    }

    public Pageable pageable(Map<String, String> params, String[] allowedSorts) {
        int size = number(params.get("size"), 20);
        if (size != 10 && size != 20 && size != 50 && size != 100) size = 20;
        int page = Math.max(0, Math.min(1_000_000, number(params.get("page"), 0)));
        String requested = params.getOrDefault("sort", "");
        String sort = Arrays.asList(allowedSorts).contains(requested) ? requested : allowedSorts[0];
        Sort.Direction direction = "asc".equalsIgnoreCase(params.get("dir")) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort order = Sort.by(direction, sort);
        if (!"id".equals(sort)) order = order.and(Sort.by(Sort.Direction.DESC, "id"));
        return PageRequest.of(page, size, order);
    }

    public <T> Page<T> query(JpaSpecificationExecutor<T> repository, Map<String, String> params, Options options) {
        return query(repository, params, options, null);
    }

    public <T> Page<T> query(JpaSpecificationExecutor<T> repository, Map<String, String> params,
                             Options options, Specification<T> required) {
        String q = trim(params.get("q"));
        String filter = trim(params.get("status"));
        Specification<T> specification = (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                Map<String, FetchParent<?, ?>> parents = new HashMap<>();
                for (String relation : options.fetches()) {
                    FetchParent<?, ?> parent = root;
                    String prefix = "";
                    for (String part : relation.split("\\.")) {
                        prefix = prefix.isEmpty() ? part : prefix + "." + part;
                        FetchParent<?, ?> next = parents.get(prefix);
                        if (next == null) {
                            next = parent.fetch(part);
                            parents.put(prefix, next);
                        }
                        parent = next;
                    }
                }
            }
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (q != null && options.search() != null) {
                String[] paths = options.search().split(",");
                String pattern = "%" + q.toLowerCase(java.util.Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
                var matches = Arrays.stream(paths).map(path -> cb.like(
                        cb.lower(resolve(root, path).as(String.class)), pattern, '!'))
                        .toArray(jakarta.persistence.criteria.Predicate[]::new);
                predicates.add(cb.or(matches));
            }
            if (filter != null && options.filter() != null) {
                Path<?> path = resolve(root, options.filter());
                Class<?> type = path.getJavaType();
                if (type.isEnum()) {
                    try {
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        Enum<?> value = Enum.valueOf((Class) type, filter);
                        predicates.add(cb.equal(path, value));
                    } catch (IllegalArgumentException invalid) {
                        predicates.add(cb.disjunction());
                    }
                } else if (type == boolean.class || type == Boolean.class) {
                    if (filter.equals("true") || filter.equals("false")) predicates.add(cb.equal(path, Boolean.valueOf(filter)));
                    else predicates.add(cb.disjunction());
                } else {
                    predicates.add(cb.equal(path.as(String.class), filter));
                }
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return repository.findAll(required == null ? specification : specification.and(required),
                pageable(params, options.sorts()));
    }

    private Path<?> resolve(Root<?> root, String dotted) {
        String[] parts = dotted.split("\\.");
        Path<?> path = root;
        for (int i = 0; i < parts.length; i++) {
            if (i < parts.length - 1 && path instanceof From<?, ?> from) path = from.join(parts[i]);
            else path = path.get(parts[i]);
        }
        return path;
    }

    public void present(Model model, Page<?> page, Map<String, String> params, Options options,
                        HttpServletRequest request, String... statusOptions) {
        model.addAttribute("adminPage", page);
        model.addAttribute("adminBase", request.getRequestURI());
        model.addAttribute("adminQuery", params.getOrDefault("q", ""));
        model.addAttribute("adminStatus", params.getOrDefault("status", ""));
        model.addAttribute("adminSort", page.getSort().iterator().next().getProperty());
        model.addAttribute("adminDir", page.getSort().iterator().next().getDirection().name().toLowerCase());
        model.addAttribute("adminSorts", options.sorts());
        model.addAttribute("adminStatuses", statusOptions);
        model.addAttribute("adminFilterLabel", filterLabel(options.filter(), options.filterLabel()));
        model.addAttribute("adminSearchPlaceholder", options.searchPlaceholder());
        model.addAttribute("adminPrev", link(request, "page", page.getNumber() - 1));
        model.addAttribute("adminNext", link(request, "page", page.getNumber() + 1));
        model.addAttribute("adminFirst", link(request, "page", 0));
        model.addAttribute("adminLast", link(request, "page", Math.max(0, page.getTotalPages() - 1)));
    }

    public String link(HttpServletRequest request, String pageKey, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(request.getRequestURI());
        request.getParameterMap().forEach((key, values) -> {
            if (!pageKey.equals(key)) for (String value : values) builder.queryParam(key, value);
        });
        return builder.queryParam(pageKey, Math.max(0, page)).build().encode().toUriString();
    }

    private int number(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ex) { return fallback; }
    }

    private String filterLabel(String filter, String fallback) {
        if (filter == null) return fallback;
        return switch (filter) {
            case "role" -> "Vai trò";
            case "active" -> "Hiển thị";
            case "type" -> "Loại nội dung";
            case "transactionType" -> "Loại giao dịch";
            default -> fallback;
        };
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
