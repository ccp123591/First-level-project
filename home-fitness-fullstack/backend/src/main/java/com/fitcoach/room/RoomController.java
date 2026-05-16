package com.fitcoach.room;

import com.fitcoach.common.ApiResult;
import com.fitcoach.common.PageResult;
import com.fitcoach.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "10. 环境建模", description = "扫描房间布局，给 AI 教练注入训练空间上下文")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomLayoutService service;

    @Operation(summary = "上传 1-3 帧扫描房间布局")
    @PostMapping(value = "/scan", consumes = "multipart/form-data")
    public ApiResult<RoomLayoutResponse> scan(@RequestParam("frames") List<MultipartFile> frames) {
        return ApiResult.ok(service.scan(SecurityUtil.currentUserId(), frames));
    }

    @Operation(summary = "获取当前用户最新房间布局")
    @GetMapping("/me")
    public ApiResult<RoomLayoutResponse> me() {
        return ApiResult.ok(service.latest(SecurityUtil.currentUserId()));
    }

    @Operation(summary = "历史扫描列表（分页）")
    @GetMapping("/history")
    public ApiResult<PageResult<RoomLayoutResponse>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.ok(service.history(SecurityUtil.currentUserId(), page, size));
    }

    @Operation(summary = "手动覆盖最新扫描的米制面积")
    @PostMapping("/me/area-override")
    public ApiResult<RoomLayoutResponse> overrideArea(@Valid @RequestBody AreaOverrideRequest req) {
        return ApiResult.ok(service.overrideArea(SecurityUtil.currentUserId(), req.getAreaSqm()));
    }

    @Data
    public static class AreaOverrideRequest {
        @NotNull
        @DecimalMin(value = "0.1", message = "areaSqm 必须 > 0")
        @DecimalMax(value = "500", message = "areaSqm 不能超过 500")
        private BigDecimal areaSqm;
    }
}
