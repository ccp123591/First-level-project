package com.fitcoach.tts;

import com.fitcoach.coach.CoachFeedback;
import com.fitcoach.coach.CoachFeedbackRepository;
import com.fitcoach.exception.BusinessException;
import com.fitcoach.infra.tts.BrowserFallbackTtsProvider;
import com.fitcoach.infra.tts.MimoTtsProvider;
import com.fitcoach.infra.tts.TtsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * TTS 编排：按 ai.tts.provider 选 mimo / browser；mimo 不可用时自动 fallback 到 browser。
 *
 * fallback 是设计上的"两个方案合一"——用户在 README 里能看到两个名字共存，
 * 但运行时 controller 永远不抛 503：要么云端真音频，要么前端朗读。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsService {

    @Value("${ai.tts.provider:browser}")
    private String configured;        // mimo | browser

    private final MimoTtsProvider mimoProvider;
    private final BrowserFallbackTtsProvider browserProvider;
    private final CoachFeedbackRepository feedbackRepo;

    /** 通用 TTS：选择当前 provider；mimo 不可用自动 fallback。 */
    public TtsResult speak(String text, String voice) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(400, "text 不能为空");
        }
        boolean wantMimo = "mimo".equalsIgnoreCase(configured);
        if (wantMimo && mimoProvider.isAvailable()) {
            try {
                return mimoProvider.speak(text, voice);
            } catch (Exception e) {
                log.warn("[tts] MiMo 调用失败，fallback 到 browser: {}", e.getMessage());
            }
        }
        return browserProvider.speak(text, voice);
    }

    /** 教练反馈语音化：拼装 review + suggestion + nextGoal。 */
    public TtsResult speakFeedback(Long userId, Long feedbackId) {
        CoachFeedback fb = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new BusinessException(404, "反馈不存在"));
        if (!Objects.equals(fb.getUserId(), userId)) {
            throw new BusinessException(403, "无权访问");
        }
        StringBuilder sb = new StringBuilder();
        if (fb.getReview() != null) sb.append(fb.getReview()).append("。");
        if (fb.getSuggestion() != null) sb.append(fb.getSuggestion()).append("。");
        if (fb.getEncouragement() != null) sb.append(fb.getEncouragement()).append("。");
        if (fb.getNextGoal() != null) sb.append(fb.getNextGoal()).append("。");
        return speak(sb.toString(), null);
    }
}
