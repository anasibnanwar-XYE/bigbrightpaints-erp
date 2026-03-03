package com.bigbrightpaints.erp.modules.admin.controller;

import com.bigbrightpaints.erp.modules.admin.dto.ChangelogEntryRequest;
import com.bigbrightpaints.erp.modules.admin.dto.ChangelogEntryResponse;
import com.bigbrightpaints.erp.modules.admin.service.ChangelogService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import com.bigbrightpaints.erp.shared.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChangelogController {

    private final ChangelogService changelogService;

    public ChangelogController(ChangelogService changelogService) {
        this.changelogService = changelogService;
    }

    @PostMapping("/admin/changelog")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ChangelogEntryResponse>> create(
            @Valid @RequestBody ChangelogEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Changelog entry created",
                changelogService.create(request)));
    }

    @PutMapping("/admin/changelog/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ChangelogEntryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ChangelogEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Changelog entry updated",
                changelogService.update(id, request)));
    }

    @DeleteMapping("/admin/changelog/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        changelogService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/changelog")
    public ResponseEntity<ApiResponse<PageResponse<ChangelogEntryResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                changelogService.list(page, size)));
    }

    @GetMapping("/changelog/latest-highlighted")
    public ResponseEntity<ApiResponse<ChangelogEntryResponse>> latestHighlighted() {
        return ResponseEntity.ok(ApiResponse.success(
                changelogService.latestHighlighted()));
    }
}
