package com.gym.management.controller;

import com.gym.management.dto.WallPostRequest;
import com.gym.management.dto.WallPostResponse;
import com.gym.management.service.WallPostService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/wall-posts")
@RequiredArgsConstructor
public class WallPostController {

    private final WallPostService wallPostService;

    @GetMapping
    public List<WallPostResponse> findActive() {
        return wallPostService.findActive();
    }

    @GetMapping("/all")
    public List<WallPostResponse> findAllForAdmin() {
        return wallPostService.findAllForAdmin();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WallPostResponse create(@Valid @RequestBody WallPostRequest request) {
        return wallPostService.create(request);
    }

    @PutMapping("/{id}")
    public WallPostResponse update(@PathVariable Long id, @Valid @RequestBody WallPostRequest request) {
        return wallPostService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        wallPostService.delete(id);
    }
}
