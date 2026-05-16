package com.fitcoach.user;

import com.fitcoach.coach.CoachFeedbackRepository;
import com.fitcoach.session.Session;
import com.fitcoach.session.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileExtractionServiceTest {

    @Mock SessionRepository sessionRepo;
    @Mock CoachFeedbackRepository feedbackRepo;
    @Mock UserProfileRepository profileRepo;
    @Mock UserRepository userRepo;

    @InjectMocks ProfileExtractionService service;

    private Session sess(String action, Integer score, Integer rhythm, Integer stability,
                         Integer depth, Integer symmetry, Integer completion, Integer reps, String date) {
        Session s = new Session();
        s.setAction(action); s.setActionLabel(zh(action)); s.setScore(score);
        s.setRhythmScore(rhythm); s.setStabilityScore(stability);
        s.setDepthScore(depth); s.setSymmetryScore(symmetry); s.setCompletionScore(completion);
        s.setReps(reps == null ? 0 : reps); s.setDuration(60); s.setSessionDate(date);
        return s;
    }

    private String zh(String a) {
        return switch (a) { case "squat" -> "深蹲"; case "pushup" -> "俯卧撑"; default -> a; };
    }

    @Test
    void aggregate_picks_favorite_action_and_weakest_dimension() {
        given(sessionRepo.findByUserIdOrderBySessionDateDesc(7L)).willReturn(List.of(
                sess("squat",  80, 90, 60, 85, 80, 95, 20, LocalDate.now().toString()),
                sess("squat",  78, 88, 55, 80, 75, 90, 20, LocalDate.now().minusDays(1).toString()),
                sess("pushup", 70, 70, 70, 70, 70, 70, 10, LocalDate.now().minusDays(2).toString())
        ));
        given(feedbackRepo.findByUserIdOrderByCreatedAtDesc(7L)).willReturn(List.of());
        given(userRepo.findById(7L)).willReturn(Optional.of(User.builder().id(7L).nickname("Alice").build()));

        UserProfileDto dto = service.aggregate(7L);

        assertThat(dto.getFavoriteAction()).isEqualTo("squat");
        assertThat(dto.getFavoriteActionLabel()).isEqualTo("深蹲");
        // depth 平均 (85+80)=82.5, stability (60+55+70)/3=61.7, rhythm (90+88+70)/3=82.7
        // 故 weakest=stability（最低）—— wait actually pushup 也算进去：
        // rhythm: (90+88+70)/3 ≈ 82.7
        // stability: (60+55+70)/3 ≈ 61.7
        // depth: (85+80+70)/3 ≈ 78.3
        // symmetry: (80+75+70)/3 = 75
        // completion: (95+90+70)/3 ≈ 85
        // → weakest = stability
        assertThat(dto.getWeakestDimension()).isEqualTo("stability");
        assertThat(dto.getTotalReps()).isEqualTo(50L);
        assertThat(dto.getSummaryText()).contains("深蹲");
        assertThat(dto.getNickname()).isEqualTo("Alice");
    }

    @Test
    void aggregate_handles_empty_session_history() {
        given(sessionRepo.findByUserIdOrderBySessionDateDesc(7L)).willReturn(List.of());
        given(feedbackRepo.findByUserIdOrderByCreatedAtDesc(7L)).willReturn(List.of());
        given(userRepo.findById(7L)).willReturn(Optional.empty());

        UserProfileDto dto = service.aggregate(7L);

        assertThat(dto.getFavoriteAction()).isNull();
        assertThat(dto.getTotalReps()).isZero();
        assertThat(dto.getStreakDays()).isZero();
        assertThat(dto.getAvgScore()).isZero();
    }

    @Test
    void refresh_upserts_profile_with_increment_version() {
        given(sessionRepo.findByUserIdOrderBySessionDateDesc(7L)).willReturn(List.of(
                sess("squat", 80, 90, 60, 85, 80, 95, 20, LocalDate.now().toString())));
        given(feedbackRepo.findByUserIdOrderByCreatedAtDesc(7L)).willReturn(List.of());
        given(userRepo.findById(7L)).willReturn(Optional.empty());
        UserProfile existing = UserProfile.builder().userId(7L).version(3).build();
        given(profileRepo.findByUserId(7L)).willReturn(Optional.of(existing));
        given(profileRepo.save(org.mockito.ArgumentMatchers.any(UserProfile.class)))
                .willAnswer(inv -> inv.getArgument(0));

        UserProfileDto dto = service.refresh(7L);

        assertThat(dto.getVersion()).isEqualTo(4);
        assertThat(dto.getSummarizer()).isEqualTo("aggregate");
        assertThat(dto.getSummaryText()).isNotBlank();
    }
}
