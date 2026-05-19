package com.gym.management.mapper;

import com.gym.management.dto.ProductResponse;
import com.gym.management.model.Product;
import java.math.BigDecimal;

public final class ProductMapper {

    private ProductMapper() {}

    public static ProductResponse toResponse(Product product) {
        BigDecimal stockValue = product.getUnitPrice()
                .multiply(BigDecimal.valueOf(product.getQuantity()));
        boolean lowStock = product.getQuantity() <= product.getMinStock();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getQuantity(),
                product.getUnitPrice(),
                stockValue,
                product.getMinStock(),
                lowStock,
                product.getActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
