package com.gym.management.controller;

import com.gym.management.dto.CarouselSlideRequest;
import com.gym.management.dto.CarouselSlideResponse;
import com.gym.management.dto.GymMediaItemRequest;
import com.gym.management.dto.GymMediaItemResponse;
import com.gym.management.dto.SiteFooterRequest;
import com.gym.management.dto.SiteFooterResponse;
import com.gym.management.dto.UploadResponse;
import com.gym.management.service.FileStorageService;
import com.gym.management.service.HomeContentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeContentController {

    private final HomeContentService homeContentService;
    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse uploadImage(@RequestParam("file") MultipartFile file) {
        return new UploadResponse(fileStorageService.storeImage(file));
    }

    @GetMapping("/carousel")
    public List<CarouselSlideResponse> findActiveCarousel() {
        return homeContentService.findActiveCarousel();
    }

    @GetMapping("/carousel/all")
    public List<CarouselSlideResponse> findAllCarousel() {
        return homeContentService.findAllCarousel();
    }

    @PostMapping("/carousel")
    @ResponseStatus(HttpStatus.CREATED)
    public CarouselSlideResponse createCarousel(@Valid @RequestBody CarouselSlideRequest request) {
        return homeContentService.createCarousel(request);
    }

    @PutMapping("/carousel/{id}")
    public CarouselSlideResponse updateCarousel(
            @PathVariable Long id, @Valid @RequestBody CarouselSlideRequest request) {
        return homeContentService.updateCarousel(id, request);
    }

    @DeleteMapping("/carousel/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCarousel(@PathVariable Long id) {
        homeContentService.deleteCarousel(id);
    }

    @GetMapping("/media")
    public List<GymMediaItemResponse> findActiveMedia() {
        return homeContentService.findActiveMedia();
    }

    @GetMapping("/media/all")
    public List<GymMediaItemResponse> findAllMedia() {
        return homeContentService.findAllMedia();
    }

    @PostMapping("/media")
    @ResponseStatus(HttpStatus.CREATED)
    public GymMediaItemResponse createMedia(@Valid @RequestBody GymMediaItemRequest request) {
        return homeContentService.createMedia(request);
    }

    @PutMapping("/media/{id}")
    public GymMediaItemResponse updateMedia(@PathVariable Long id, @Valid @RequestBody GymMediaItemRequest request) {
        return homeContentService.updateMedia(id, request);
    }

    @DeleteMapping("/media/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(@PathVariable Long id) {
        homeContentService.deleteMedia(id);
    }

    @GetMapping("/footer")
    public SiteFooterResponse getFooter() {
        return homeContentService.getFooter();
    }

    @PutMapping("/footer")
    public SiteFooterResponse updateFooter(@Valid @RequestBody SiteFooterRequest request) {
        return homeContentService.updateFooter(request);
    }
}
