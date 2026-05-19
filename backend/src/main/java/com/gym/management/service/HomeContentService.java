package com.gym.management.service;

import com.gym.management.dto.CarouselSlideRequest;
import com.gym.management.dto.CarouselSlideResponse;
import com.gym.management.dto.GymMediaItemRequest;
import com.gym.management.dto.GymMediaItemResponse;
import com.gym.management.dto.SiteFooterRequest;
import com.gym.management.dto.SiteFooterResponse;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.CarouselSlide;
import com.gym.management.model.GymMediaItem;
import com.gym.management.model.SiteFooter;
import com.gym.management.repository.CarouselSlideRepository;
import com.gym.management.repository.GymMediaItemRepository;
import com.gym.management.repository.SiteFooterRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeContentService {

    private final CarouselSlideRepository carouselSlideRepository;
    private final GymMediaItemRepository gymMediaItemRepository;
    private final SiteFooterRepository siteFooterRepository;

    @Transactional(readOnly = true)
    public List<CarouselSlideResponse> findActiveCarousel() {
        return carouselSlideRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(this::toCarouselResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CarouselSlideResponse> findAllCarousel() {
        return carouselSlideRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(this::toCarouselResponse)
                .toList();
    }

    @Transactional
    public CarouselSlideResponse createCarousel(CarouselSlideRequest request) {
        CarouselSlide slide = CarouselSlide.builder()
                .imageUrl(request.imageUrl().trim())
                .title(blankToNull(request.title()))
                .caption(blankToNull(request.caption()))
                .linkUrl(blankToNull(request.linkUrl()))
                .displayOrder(resolveOrder(request.displayOrder(), carouselSlideRepository.count()))
                .active(request.active() == null || request.active())
                .build();
        return toCarouselResponse(carouselSlideRepository.save(slide));
    }

    @Transactional
    public CarouselSlideResponse updateCarousel(Long id, CarouselSlideRequest request) {
        CarouselSlide slide = getCarousel(id);
        slide.setImageUrl(request.imageUrl().trim());
        slide.setTitle(blankToNull(request.title()));
        slide.setCaption(blankToNull(request.caption()));
        slide.setLinkUrl(blankToNull(request.linkUrl()));
        if (request.displayOrder() != null) {
            slide.setDisplayOrder(request.displayOrder());
        }
        if (request.active() != null) {
            slide.setActive(request.active());
        }
        return toCarouselResponse(carouselSlideRepository.save(slide));
    }

    @Transactional
    public void deleteCarousel(Long id) {
        if (!carouselSlideRepository.existsById(id)) {
            throw new ResourceNotFoundException("Imagen del slider no encontrada: " + id);
        }
        carouselSlideRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<GymMediaItemResponse> findActiveMedia() {
        return gymMediaItemRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(this::toMediaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GymMediaItemResponse> findAllMedia() {
        return gymMediaItemRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(this::toMediaResponse)
                .toList();
    }

    @Transactional
    public GymMediaItemResponse createMedia(GymMediaItemRequest request) {
        GymMediaItem item = GymMediaItem.builder()
                .mediaType(request.mediaType())
                .mediaUrl(request.mediaUrl().trim())
                .thumbnailUrl(blankToNull(request.thumbnailUrl()))
                .title(blankToNull(request.title()))
                .displayOrder(resolveOrder(request.displayOrder(), gymMediaItemRepository.count()))
                .active(request.active() == null || request.active())
                .build();
        return toMediaResponse(gymMediaItemRepository.save(item));
    }

    @Transactional
    public GymMediaItemResponse updateMedia(Long id, GymMediaItemRequest request) {
        GymMediaItem item = getMedia(id);
        item.setMediaType(request.mediaType());
        item.setMediaUrl(request.mediaUrl().trim());
        item.setThumbnailUrl(blankToNull(request.thumbnailUrl()));
        item.setTitle(blankToNull(request.title()));
        if (request.displayOrder() != null) {
            item.setDisplayOrder(request.displayOrder());
        }
        if (request.active() != null) {
            item.setActive(request.active());
        }
        return toMediaResponse(gymMediaItemRepository.save(item));
    }

    @Transactional
    public void deleteMedia(Long id) {
        if (!gymMediaItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Medio no encontrado: " + id);
        }
        gymMediaItemRepository.deleteById(id);
    }

    @Transactional
    public SiteFooterResponse getFooter() {
        return toFooterResponse(getOrCreateFooter());
    }

    @Transactional
    public SiteFooterResponse updateFooter(SiteFooterRequest request) {
        SiteFooter footer = getOrCreateFooter();
        footer.setTagline(blankToNull(request.tagline()));
        footer.setAddress(blankToNull(request.address()));
        footer.setPhone(blankToNull(request.phone()));
        footer.setInstagramUrl(blankToNull(request.instagramUrl()));
        footer.setFacebookUrl(blankToNull(request.facebookUrl()));
        footer.setTiktokUrl(blankToNull(request.tiktokUrl()));
        footer.setYoutubeUrl(blankToNull(request.youtubeUrl()));
        footer.setWhatsappUrl(blankToNull(request.whatsappUrl()));
        return toFooterResponse(siteFooterRepository.save(footer));
    }

    private CarouselSlide getCarousel(Long id) {
        return carouselSlideRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen del slider no encontrada: " + id));
    }

    private GymMediaItem getMedia(Long id) {
        return gymMediaItemRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medio no encontrado: " + id));
    }

    private SiteFooter getOrCreateFooter() {
        return siteFooterRepository
                .findById(SiteFooter.SINGLETON_ID)
                .orElseGet(() -> siteFooterRepository.save(SiteFooter.builder()
                        .id(SiteFooter.SINGLETON_ID)
                        .tagline("Sport Gym R.IO — Tu espacio para entrenar")
                        .build()));
    }

    private int resolveOrder(Integer requested, long count) {
        return requested != null ? requested : (int) count;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private CarouselSlideResponse toCarouselResponse(CarouselSlide slide) {
        return new CarouselSlideResponse(
                slide.getId(),
                slide.getImageUrl(),
                slide.getTitle(),
                slide.getCaption(),
                slide.getLinkUrl(),
                slide.getDisplayOrder(),
                slide.isActive(),
                slide.getCreatedAt());
    }

    private GymMediaItemResponse toMediaResponse(GymMediaItem item) {
        return new GymMediaItemResponse(
                item.getId(),
                item.getMediaType(),
                item.getMediaUrl(),
                item.getThumbnailUrl(),
                item.getTitle(),
                item.getDisplayOrder(),
                item.isActive(),
                item.getCreatedAt());
    }

    private SiteFooterResponse toFooterResponse(SiteFooter footer) {
        return new SiteFooterResponse(
                footer.getTagline(),
                footer.getAddress(),
                footer.getPhone(),
                footer.getInstagramUrl(),
                footer.getFacebookUrl(),
                footer.getTiktokUrl(),
                footer.getYoutubeUrl(),
                footer.getWhatsappUrl(),
                footer.getUpdatedAt());
    }
}
