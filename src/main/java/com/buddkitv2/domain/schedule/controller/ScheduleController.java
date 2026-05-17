package com.buddkitv2.domain.schedule.controller;

import com.buddkitv2.domain.schedule.dto.request.ScheduleCreateRequest;
import com.buddkitv2.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.buddkitv2.domain.schedule.dto.response.ScheduleMemberResponse;
import com.buddkitv2.domain.schedule.dto.response.ScheduleResponse;
import com.buddkitv2.domain.schedule.service.ScheduleService;
import com.buddkitv2.domain.settlement.dto.response.SettlementStatusResponse;
import com.buddkitv2.domain.settlement.service.SettlementService;
import com.buddkitv2.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clubs/{clubId}/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final SettlementService settlementService;

    // ── 스케줄 CRUD ────────────────────────────────────────

    @PostMapping
    public ApiResponse<ScheduleResponse> createSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @RequestBody @Valid ScheduleCreateRequest request) {
        return ApiResponse.ok(scheduleService.createSchedule(userId, clubId, request));
    }

    @PatchMapping("/{scheduleId}")
    public ApiResponse<ScheduleResponse> updateSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @RequestBody @Valid ScheduleUpdateRequest request) {
        return ApiResponse.ok(scheduleService.updateSchedule(userId, clubId, scheduleId, request));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(userId, clubId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ApiResponse<List<ScheduleResponse>> getSchedules(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(scheduleService.getSchedules(userId, clubId, lastId, size));
    }

    @GetMapping("/{scheduleId}")
    public ApiResponse<ScheduleResponse> getSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        return ApiResponse.ok(scheduleService.getSchedule(userId, clubId, scheduleId));
    }

    // ── 참여 ──────────────────────────────────────────────

    @PostMapping("/{scheduleId}/members")
    public ResponseEntity<Void> joinSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        scheduleService.joinSchedule(userId, clubId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{scheduleId}/members/me")
    public ResponseEntity<Void> leaveSchedule(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        scheduleService.leaveSchedule(userId, clubId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{scheduleId}/members")
    public ApiResponse<List<ScheduleMemberResponse>> getScheduleMembers(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(scheduleService.getScheduleMembers(userId, clubId, scheduleId, lastId, size));
    }

    // ── 정산 ──────────────────────────────────────────────

    @PostMapping("/{scheduleId}/settlements")
    public ResponseEntity<Void> requestSettlement(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        settlementService.requestSettlement(userId, clubId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{scheduleId}/settlements")
    public ApiResponse<List<SettlementStatusResponse>> getSettlements(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(settlementService.getSettlements(userId, clubId, scheduleId, lastId, size));
    }

    @PostMapping("/{scheduleId}/settlements/me")
    public ResponseEntity<Void> settleMyShare(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        settlementService.settleMyShare(userId, clubId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{scheduleId}/settlements/me/reserve")
    public ResponseEntity<Void> reserveMyShare(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId) {
        settlementService.reserveMyShare(userId, clubId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{scheduleId}/settlements/{userSettlementId}")
    public ResponseEntity<Void> completeManually(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long clubId,
            @PathVariable Long scheduleId,
            @PathVariable Long userSettlementId) {
        settlementService.completeManually(userId, clubId, scheduleId, userSettlementId);
        return ResponseEntity.noContent().build();
    }
}
