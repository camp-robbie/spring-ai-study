package com.example.kakaochat.service;

import com.example.kakaochat.model.ChatRequest;
import com.example.kakaochat.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * ============================================================
 *  채팅 서비스 — 프롬프트 기법 실습 가이드
 * ============================================================
 *
 *  이 파일의 chat() 메서드를 수정하면서 다양한 프롬프트 기법을 실험하세요.
 *  각 기법별 예시 코드가 주석으로 포함되어 있습니다.
 *
 *  수정 후 애플리케이션을 재시작하면 변경사항이 반영됩니다.
 *  (DevTools가 있으므로 자동 재시작될 수도 있습니다)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;

    // ================================================================
    //  기본 채팅 — 이 메서드를 수정해가면서 테스트 진행해보세요.
    // ================================================================
    public ChatResponse chat(ChatRequest request) {
        try {
            /**
             * Zero-shot Prompting
             * 감정 분석(긍정/부정/중립)은 LLM이 사전학습에서 이미 충분히 학습한 대표적 태스크입니다.
             * 별도의 예시 없이 지시만으로도 높은 정확도를 보이기 때문에 Zero-shot이 가장 효율적입니다.
             * 예시를 넣으면 오히려 토큰 낭비이며, 모델이 예시 패턴에 과적합될 위험이 있습니다.
             *
             * 테스트 예시:
             *   "이 식당 진짜 맛있어요! 또 올 거예요"      → 긍정
             *   "배송이 너무 늦고 포장도 엉망이네요"       → 부정
             *   "오늘 택배 왔습니다"                   → 중립
             */

            /*
            log.info("[Zero-shot] request: {}", request.getMessage());

            // Zero-shot: 예시 없이, 명확한 지시(instruction)만 제공
            String response = chatClient.prompt()
                    .system("""
                            당신은 고객 리뷰 감정 분석기입니다.
                            사용자가 입력한 리뷰의 감정을 분석하세요.

                            반드시 아래 형식으로만 답변하세요:
                            감정: [긍정/부정/중립]
                            신뢰도: [높음/중간/낮음]
                            핵심 키워드: [감정을 판단한 근거가 된 단어들]
                            """)
                    .user(request.getMessage())
                    .call()
                    .content();

             */


            /**
             * Few-shot Prompting
             * 사내 CS 답변은 회사마다 고유한 말투·형식·정책이 있어 LLM이 사전학습만으로는 알 수 없습니다.
             * 2~3개의 예시(Q&A 쌍)를 보여주면 모델이 "우리 회사 톤앤매너"를 즉시 파악합니다.
             * Zero-shot으로는 일반적인 CS 답변만 나오지만, Few-shot은 브랜드 일관성을 유지합니다.
             *
             * 테스트 예시:
             *   "환불은 어떻게 하나요?"
             *   "배송이 안 왔어요"
             *   "제품에 하자가 있어요"
             */

            /*
            log.info("[Few-shot] request: {}", request.getMessage());

            // Few-shot: 예시를 포함하여 원하는 형식과 톤을 학습시킴
            String response = chatClient.prompt()
                    .system("""
                            당신은 온라인 쇼핑몰 '해피마켓'의 고객상담 AI입니다.
                            아래 예시와 동일한 톤과 형식으로 답변하세요.

                            === 예시 1 ===
                            고객: 주문 취소하고 싶어요
                            상담원: 안녕하세요, 해피마켓입니다! 😊
                            주문 취소를 도와드리겠습니다.

                            📌 취소 방법:
                            1. [마이페이지] → [주문내역]에서 해당 주문의 [취소] 버튼을 눌러주세요.
                            2. 이미 발송된 경우, [반품 신청]으로 진행해 주세요.

                            💡 환불은 취소 후 영업일 기준 1~3일 내 처리됩니다.
                            추가 궁금한 점이 있으시면 편하게 말씀해 주세요!

                            === 예시 2 ===
                            고객: 포인트 언제 적립되나요?
                            상담원: 안녕하세요, 해피마켓입니다! 😊
                            포인트 관련 문의 감사합니다.

                            📌 포인트 적립 안내:
                            1. 구매 확정 후 자동으로 적립됩니다 (결제금액의 1%).
                            2. 배송 완료 후 7일 이내에 자동 구매 확정됩니다.

                            💡 적립된 포인트는 [마이페이지] → [포인트]에서 확인 가능합니다.
                            추가 궁금한 점이 있으시면 편하게 말씀해 주세요!

                            === 규칙 ===
                            - 항상 "안녕하세요, 해피마켓입니다! 😊"로 시작
                            - 📌로 핵심 안내, 💡로 보충 팁 제공
                            - 마지막에 추가 질문 유도
                            """)
                    .user("고객: " + request.getMessage())
                    .call()
                    .content();

             */

            /**
             * Step-back Prompting
             * "스프링 컨테이너가 왜 필요한가?" 같은 기술 질문은 바로 답하면 피상적이 됩니다.
             * Step-back은 먼저 "상위 원리(의존성 관리, 객체 생명주기)"를 생각한 뒤,
             * 그 원리를 기반으로 구체적 답변을 도출합니다.
             * 이 기법은 복잡한 개념 설명이나 "왜?"라는 질문에 특히 효과적입니다.
             *
             * 테스트 예시:
             *   "자바에서 왜 인터페이스를 사용하나요?"
             *   "마이크로서비스가 왜 필요한가요?"
             *   "왜 데이터베이스 인덱스를 걸어야 하나요?"
             */


            log.info("[Step-back] request: {}", request.getMessage());

            // Step-back: 2단계로 분해 — 먼저 상위 원리를 도출하고, 그 위에서 구체적 답변을 생성
            // 1단계: 상위 개념 추출
            String principles = chatClient.prompt()
                    .system("""
                            당신은 컴퓨터 과학 교수입니다.
                            사용자의 질문을 보고, 이 질문의 답을 이해하기 위해
                            먼저 알아야 할 "상위 원리" 또는 "배경 개념" 2~3가지를 짧게 정리하세요.

                            형식:
                            - 원리 1: [한 줄 설명]
                            - 원리 2: [한 줄 설명]
                            - 원리 3: [한 줄 설명]
                            """)
                    .user(request.getMessage())
                    .call()
                    .content();

            log.info("[Step-back] 상위 원리 추출 완료: {}", principles);

            // 2단계: 상위 원리를 바탕으로 본질적인 답변 생성
            String preResponse = chatClient.prompt()
                    .system("""
                            당신은 컴퓨터 과학 교수입니다.
                            아래 [배경 원리]를 바탕으로 학생의 질문에 답변하세요.
                            반드시 원리와 연결지어 설명하고, 실무 예시를 포함하세요.
                            """)
                    .user("""
                            [배경 원리]
                            %s

                            [학생 질문]
                            %s
                            """.formatted(principles, request.getMessage()))
                    .call()
                    .content();

            // 최종 응답: 상위 원리 + 본 답변을 함께 반환
            String response = """
                    [Step-back: 상위 원리 분석]
                    %s

                    ─────────────────────────────

                    [원리 기반 답변]
                    %s
                    """.formatted(principles, preResponse);


            log.info("Chat response length: {}", response != null ? response.length() : 0);
            return ChatResponse.success(response);

        } catch (Exception e) {
            log.error("Chat error: ", e);
            return ChatResponse.error("AI 응답 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // ================================================================
    //  스트리밍 채팅 — 실시간 토큰 전송 (SSE)
    //  이 메서드를 수정해가면서 테스트 진행해보세요.
    // ================================================================
    public Flux<String> streamChat(ChatRequest request) {
        log.info("Stream chat request: {}", request.getMessage());

        return chatClient.prompt()
                .user(request.getMessage())
                .stream()
                .content();
    }
}
