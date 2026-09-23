package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.dto.HomeContentRequest;
import com.luismunozse.reservalago.dto.HomeContentResponse;
import com.luismunozse.reservalago.service.HomeContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeContentController {

    private final HomeContentService homeContentService;

    @GetMapping("/api/home/content")
    public HomeContentResponse getPublicContent() {
        return homeContentService.getContent();
    }

    @GetMapping("/api/admin/home/content")
    public HomeContentResponse getAdminContent() {
        return homeContentService.getContent();
    }

    @PutMapping("/api/admin/home/content")
    public HomeContentResponse updateContent(@Valid @RequestBody HomeContentRequest request) {
        return homeContentService.updateContent(request);
    }
}
