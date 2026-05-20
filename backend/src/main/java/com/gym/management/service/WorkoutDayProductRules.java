package com.gym.management.service;

import com.gym.management.model.Product;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Productos de venta = plan de entreno por un solo día (pase diario). */
@Component
public class WorkoutDayProductRules {

    private final Set<String> categories;
    private final Set<String> nameKeywords;

    public WorkoutDayProductRules(
            @Value("${app.sales.workout-day-product-categories:ENTRENO_DIA,ENTRENO,Plan entreno}") String categoriesCsv,
            @Value("${app.sales.workout-day-name-keywords:entreno,pase,día,dia}") String keywordsCsv) {
        this.categories = parseCsv(categoriesCsv);
        this.nameKeywords = parseCsv(keywordsCsv);
    }

    public boolean isWorkoutDayProduct(Product product) {
        if (product == null) {
            return false;
        }
        String category = product.getCategory();
        if (category != null && !category.isBlank()) {
            String normalized = normalize(category);
            if (categories.contains(normalized)) {
                return true;
            }
        }
        String name = product.getName();
        if (name == null || name.isBlank()) {
            return false;
        }
        String normalizedName = normalize(name);
        return nameKeywords.stream().anyMatch(normalizedName::contains);
    }

    private static Set<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(WorkoutDayProductRules::normalize)
                .collect(Collectors.toSet());
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
