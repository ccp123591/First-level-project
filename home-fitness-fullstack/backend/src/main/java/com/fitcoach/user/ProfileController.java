package com.fitcoach.user;

import com.fitcoach.common.ApiResult;
import com.fitcoach.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "09. 用户画像", description = "聚合训练数据生成的用户画像，供 AI 教练读取")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users/me/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileExtractionService service;

    @Operation(summary = "读取当前用户画像（缺失时现场聚合）")
    @GetMapping
    public ApiResult<UserProfileDto> me() {
        return ApiResult.ok(service.read(SecurityUtil.currentUserId()));
    }

    @Operation(summary = "强制刷新当前用户画像")
    @PostMapping("/refresh")
    public ApiResult<UserProfileDto> refresh() {
        return ApiResult.ok(service.refresh(SecurityUtil.currentUserId()));
    }
}
