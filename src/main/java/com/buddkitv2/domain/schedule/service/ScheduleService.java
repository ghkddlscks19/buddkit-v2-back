package com.buddkitv2.domain.schedule.service;

import com.buddkitv2.domain.chat.service.ChatService;
import com.buddkitv2.domain.club.entity.UserClub;
import com.buddkitv2.domain.club.entity.UserClubRole;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.schedule.dto.request.ScheduleCreateRequest;
import com.buddkitv2.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.buddkitv2.domain.schedule.dto.response.ScheduleMemberResponse;
import com.buddkitv2.domain.schedule.dto.response.ScheduleResponse;
import com.buddkitv2.domain.schedule.entity.Schedule;
import com.buddkitv2.domain.schedule.entity.ScheduleStatus;
import com.buddkitv2.domain.schedule.entity.UserSchedule;
import com.buddkitv2.domain.schedule.entity.UserScheduleRole;
import com.buddkitv2.domain.schedule.repository.ScheduleRepository;
import com.buddkitv2.domain.schedule.repository.UserScheduleRepository;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.exception.AlreadyJoinedScheduleException;
import com.buddkitv2.global.exception.NotJoinedScheduleException;
import com.buddkitv2.global.exception.ScheduleAccessDeniedException;
import com.buddkitv2.global.exception.ScheduleFullException;
import com.buddkitv2.global.exception.ScheduleNotFoundException;
import com.buddkitv2.global.exception.ScheduleNotRecruitingException;
import com.buddkitv2.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserScheduleRepository userScheduleRepository;
    private final UserClubRepository userClubRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;

    // ── 헬퍼 ──────────────────────────────────────────────

    private UserClub requireClubMember(Long clubId, Long userId) {
        return userClubRepository.findByClub_IdAndUser_Id(clubId, userId)
                .orElseThrow(ScheduleAccessDeniedException::new);
    }

    private void requireLeader(UserClub userClub) {
        if (userClub.getRole() != UserClubRole.LEADER) {
            throw new ScheduleAccessDeniedException();
        }
    }

    private Schedule findActiveSchedule(Long clubId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findActiveById(scheduleId)
                .orElseThrow(ScheduleNotFoundException::new);
        if (!schedule.getClub().getId().equals(clubId)) {
            throw new ScheduleNotFoundException();
        }
        return schedule;
    }

    private ScheduleResponse toResponse(Schedule schedule, Long userId) {
        long count = userScheduleRepository.countBySchedule_Id(schedule.getId());
        boolean joined = userScheduleRepository.existsBySchedule_IdAndUser_Id(schedule.getId(), userId);
        return new ScheduleResponse(
                schedule.getId(), schedule.getName(), schedule.getScheduleTime(),
                schedule.getLocation(), schedule.getCost(), schedule.getStatus(),
                schedule.getLimit(), count, joined
        );
    }

    // ── CRUD ──────────────────────────────────────────────

    @Transactional
    public ScheduleResponse createSchedule(Long userId, Long clubId, ScheduleCreateRequest req) {
        UserClub userClub = requireClubMember(clubId, userId);
        requireLeader(userClub);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Schedule schedule = Schedule.create(req.getName(), req.getScheduleTime(),
                req.getLocation(), req.getCost(), req.getLimit(), userClub.getClub());
        scheduleRepository.save(schedule);
        userScheduleRepository.save(UserSchedule.create(user, schedule, UserScheduleRole.LEADER));
        chatService.createChatRoomForSchedule(userClub.getClub(), schedule.getId(), user);
        return toResponse(schedule, userId);
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long userId, Long clubId, Long scheduleId, ScheduleUpdateRequest req) {
        UserClub userClub = requireClubMember(clubId, userId);
        requireLeader(userClub);
        Schedule schedule = findActiveSchedule(clubId, scheduleId);
        schedule.update(req.getName(), req.getScheduleTime(), req.getLocation(), req.getCost(), req.getLimit());
        return toResponse(schedule, userId);
    }

    @Transactional
    public void deleteSchedule(Long userId, Long clubId, Long scheduleId) {
        UserClub userClub = requireClubMember(clubId, userId);
        requireLeader(userClub);
        Schedule schedule = findActiveSchedule(clubId, scheduleId);
        schedule.softDelete();
        chatService.deleteChatRoomsByScheduleId(scheduleId);
    }

    // ── 조회 ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedules(Long userId, Long clubId, Long lastId, int size) {
        requireClubMember(clubId, userId);
        List<Schedule> schedules = lastId == null
                ? scheduleRepository.findByClubId(clubId, PageRequest.of(0, size))
                : scheduleRepository.findByClubIdAndLastId(clubId, lastId, PageRequest.of(0, size));
        return schedules.stream().map(s -> toResponse(s, userId)).toList();
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(Long userId, Long clubId, Long scheduleId) {
        requireClubMember(clubId, userId);
        Schedule schedule = findActiveSchedule(clubId, scheduleId);
        return toResponse(schedule, userId);
    }

    // ── 참여 ──────────────────────────────────────────────

    @Transactional
    public void joinSchedule(Long userId, Long clubId, Long scheduleId) {
        requireClubMember(clubId, userId);
        Schedule schedule = findActiveSchedule(clubId, scheduleId);
        if (schedule.getStatus() != ScheduleStatus.RECRUITING) {
            throw new ScheduleNotRecruitingException();
        }
        if (userScheduleRepository.existsBySchedule_IdAndUser_Id(scheduleId, userId)) {
            throw new AlreadyJoinedScheduleException();
        }
        if (userScheduleRepository.countBySchedule_Id(scheduleId) >= schedule.getLimit()) {
            throw new ScheduleFullException();
        }
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        userScheduleRepository.save(UserSchedule.create(user, schedule, UserScheduleRole.MEMBER));
        chatService.addScheduleMember(scheduleId, user);
    }

    @Transactional
    public void leaveSchedule(Long userId, Long clubId, Long scheduleId) {
        requireClubMember(clubId, userId);
        findActiveSchedule(clubId, scheduleId);
        UserSchedule userSchedule = userScheduleRepository.findBySchedule_IdAndUser_Id(scheduleId, userId)
                .orElseThrow(NotJoinedScheduleException::new);
        if (userSchedule.getRole() == UserScheduleRole.LEADER) {
            throw new ScheduleAccessDeniedException();
        }
        userScheduleRepository.delete(userSchedule);
        chatService.removeScheduleMember(scheduleId, userId);
    }

    // ── 참여자 목록 ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ScheduleMemberResponse> getScheduleMembers(Long userId, Long clubId, Long scheduleId,
                                                            Long lastId, int size) {
        requireClubMember(clubId, userId);
        findActiveSchedule(clubId, scheduleId);
        List<UserSchedule> list = lastId == null
                ? userScheduleRepository.findByScheduleId(scheduleId, PageRequest.of(0, size))
                : userScheduleRepository.findByScheduleIdAndLastId(scheduleId, lastId, PageRequest.of(0, size));
        return list.stream().map(us -> new ScheduleMemberResponse(
                us.getId(), us.getUser().getId(), us.getUser().getNickname(),
                us.getUser().getProfileImageUrl(), us.getRole()
        )).toList();
    }
}
