package com.gym.management.service;

import com.gym.management.dto.ProductRequest;
import com.gym.management.dto.ProductResponse;
import com.gym.management.dto.StockAdjustmentRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.ProductMapper;
import com.gym.management.model.Product;
import com.gym.management.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductMapper.toResponse(getProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = mapRequest(new Product(), request);
        return ProductMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        mapRequest(product, request);
        return ProductMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse adjustStock(Long id, StockAdjustmentRequest request) {
        Product product = getProduct(id);
        int newQuantity = product.getQuantity() + request.delta();
        if (newQuantity < 0) {
            throw new BusinessException("No hay stock suficiente para esta operación");
        }
        product.setQuantity(newQuantity);
        return ProductMapper.toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado: " + id);
        }
        productRepository.deleteById(id);
    }

    private Product mapRequest(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setCategory(request.category());
        product.setQuantity(request.quantity());
        product.setUnitPrice(request.unitPrice());
        product.setMinStock(request.minStock() != null ? request.minStock() : 0);
        if (request.active() != null) {
            product.setActive(request.active());
        } else if (product.getActive() == null) {
            product.setActive(true);
        }
        return product;
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }
}
