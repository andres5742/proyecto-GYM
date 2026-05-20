package com.gym.management.controller;

import com.gym.management.dto.ProductCreditPayAllRequest;
import com.gym.management.dto.ProductCreditPayAllResponse;
import com.gym.management.dto.ProductCreditPaymentRequest;
import com.gym.management.dto.ProductCreditPaymentResponse;
import com.gym.management.dto.ProductCreditRequest;
import com.gym.management.dto.ProductCreditResponse;
import com.gym.management.model.ProductCreditStatus;
import com.gym.management.service.ProductCreditService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-credits")
@RequiredArgsConstructor
public class ProductCreditController {

    private final ProductCreditService productCreditService;

    @GetMapping
    public List<ProductCreditResponse> findAll(
            @RequestParam(required = false) ProductCreditStatus status) {
        return productCreditService.findAll(status);
    }

    @PostMapping("/member/{memberId}/pay-all")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductCreditPayAllResponse payAllForMember(
            @PathVariable Long memberId, @Valid @RequestBody ProductCreditPayAllRequest request) {
        return productCreditService.payAllForMember(memberId, request);
    }

    @GetMapping("/{id}")
    public ProductCreditResponse findById(@PathVariable Long id) {
        return productCreditService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductCreditResponse create(@Valid @RequestBody ProductCreditRequest request) {
        return productCreditService.create(request);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductCreditPaymentResponse registerPayment(
            @PathVariable Long id, @Valid @RequestBody ProductCreditPaymentRequest request) {
        return productCreditService.registerPayment(id, request);
    }
}
