package com.gym.management.service;

import com.gym.management.model.CarouselSlide;
import com.gym.management.model.WallPost;
import com.gym.management.model.WallPostImage;
import com.gym.management.repository.CarouselSlideRepository;
import com.gym.management.repository.WallPostRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Elimina referencias en BD a archivos /uploads que ya no existen en disco. */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadReferenceCleanupService {

    private final FileStorageService fileStorageService;
    private final WallPostRepository wallPostRepository;
    private final CarouselSlideRepository carouselSlideRepository;

    @Transactional
    public void purgeMissingUploadReferences() {
        int removedImages = purgeWallPostImages();
        int deactivatedSlides = deactivateBrokenCarouselSlides();
        if (removedImages > 0 || deactivatedSlides > 0) {
            log.info(
                    "Referencias de uploads huérfanas: {} imagen(es) de muro, {} slide(s) de carrusel desactivado(s)",
                    removedImages,
                    deactivatedSlides);
        }
    }

    private int purgeWallPostImages() {
        int removed = 0;
        for (WallPost post : wallPostRepository.findAllWithAuthor()) {
            List<WallPostImage> orphans = new ArrayList<>();
            for (WallPostImage image : post.getImages()) {
                if (!fileStorageService.localUploadExists(image.getImageUrl())) {
                    orphans.add(image);
                }
            }
            if (!orphans.isEmpty()) {
                post.getImages().removeAll(orphans);
                wallPostRepository.save(post);
                removed += orphans.size();
            }
        }
        return removed;
    }

    private int deactivateBrokenCarouselSlides() {
        int deactivated = 0;
        for (CarouselSlide slide : carouselSlideRepository.findAll()) {
            if (slide.isActive()
                    && fileStorageService.isLocalUploadUrl(slide.getImageUrl())
                    && !fileStorageService.localUploadExists(slide.getImageUrl())) {
                slide.setActive(false);
                carouselSlideRepository.save(slide);
                deactivated++;
            }
        }
        return deactivated;
    }
}
