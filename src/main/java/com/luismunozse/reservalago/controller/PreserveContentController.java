package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.dto.PreserveContentRequest;
import com.luismunozse.reservalago.dto.PreserveContentResponse;
import com.luismunozse.reservalago.service.PreserveContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PreserveContentController {

    private final PreserveContentService preserveContentService;

    @GetMapping("/api/preservar/content")
    public PreserveContentResponse getPublicContent() {
        return preserveContentService.getContent();
    }

    @GetMapping("/api/admin/preservar/content")
    public PreserveContentResponse getAdminContent() {
        return preserveContentService.getContent();
    }

    @PutMapping("/api/admin/preservar/content")
    public PreserveContentResponse updateContent(@Valid @RequestBody PreserveContentRequest request) {
        return preserveContentService.updateContent(request);
    }
}
