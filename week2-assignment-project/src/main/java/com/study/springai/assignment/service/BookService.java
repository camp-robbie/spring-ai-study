package com.study.springai.assignment.service;

import com.study.springai.assignment.dto.BookAnalysisRequest;
import com.study.springai.assignment.dto.BookRecommendRequest;
import com.study.springai.assignment.dto.BookRecommendation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
public class BookService {
    private final ChatModel chatModel;
    private final ChatClient chatClient;

    public BookService(ChatClient chatClient, ChatModel chatModel) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
    }

    private String getBookRecommendationPrompt(BookRecommendRequest request) {
        String book;

        if (request.count() > 1) {
            book = "%s books".formatted(request.count());
        }else {
            book = "a book";
        }

        return """
            Hello, I'd like you to recommend me %s.

            Genre needs to be %s.
            And the mood needs to be %s.
        """.formatted(book, request.genre(), request.mood());
    }

    // 1: 도서 추천 기능 구현 (Ch2 - 텍스트 대화)
    // ChatClient의 Fluent API(prompt → user → call → content)를 사용하여
    // request의 genre, mood, count 정보를 포함한 프롬프트를 작성하고 응답을 반환.
    public String recommendBooks(BookRecommendRequest request) {
        String message = getBookRecommendationPrompt(request);

        return this.chatClient.prompt()
            .user(message)
            .call()
            .content();
    }

    // 2: 도서 분석 기능 구현 (Ch3 - 프롬프트 템플릿)
    // prompts/book-analysis.st 템플릿 파일을 ClassPathResource로 로드한 뒤
    // PromptTemplate을 사용하여 변수를 치환하고 ChatClient로 실행.
    public String analyzeBook(BookAnalysisRequest request) {
        ClassPathResource resource = new ClassPathResource("prompts/book-analysis.st");
        PromptTemplate template = new PromptTemplate(resource);

        String prompt = template.create(Map.of(
            "title", request.title(),
            "author", request.author()
        )).getContents();

        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }

    // 3: 구조화된 도서 추천 기능 구현 (Ch4 - 구조화된 출력)
    // ChatClient의 entity() 메서드와 ParameterizedTypeReference를 사용하여
    // 응답을 List<BookRecommendation> 형태로 변환하여 반환하세요.
    public List<BookRecommendation> getStructuredRecommendations(BookRecommendRequest request) {
        String message = getBookRecommendationPrompt(request);

        return chatClient.prompt()
            .user(message)
            .call()
            .entity(new ParameterizedTypeReference<List<BookRecommendation>>() {});
    }

    // 4: 제로-샷 도서 분류 기능 구현 (프롬프트 엔지니어링)
    // 예시를 제공하지 않고, 명확한 지시문만으로 도서 설명의 장르를 분류하세요.
    // 분류 카테고리와 분류 기준을 프롬프트에 명시하는 것이 핵심입니다.
    public String classifyBookZeroShot(String bookDescription) {
        String message = """
            You will be given an description about a certain book.
            And your job is to identify the book's genre from that description.

            Keep your answer brief since it's for book categorization.

            And you are allowed to answer 'unknown' if you are truly not sure what the genre could be.
            Or if the following text is not a description about a book.
        """;

        return chatClient.prompt()
            .system(message)
            .user(bookDescription)
            .call()
            .content();
    }

    // 5: 스텝-백 도서 분석 기능 구현 (프롬프트 엔지니어링)
    // 구체적 질문에 바로 답하지 않고, 먼저 상위 개념(배경, 원리)을 탐색한 뒤
    // 이를 바탕으로 답변을 도출하는 단계적 프롬프트를 설계하세요.
    public String analyzeWithStepBack(String title, String question) {
        String initMessage = """
            What do you think is a good way to analyze a certain book?
        """;

        String questionMessage = """
            Given your answer, I'd like you to answer a question about book called : '{title}'

            Question is this:

            {question}
        """;

        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
            .maxMessages(10)
            .build();

        ChatClient tmpClient = ChatClient.builder(chatModel)
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
            .build();

        tmpClient.prompt()
            .user(initMessage)
            .call();

        return tmpClient
            .prompt()
            .user(u -> u.text(questionMessage)
                    .param("title", title)
                    .param("question", question)
             )
            .call()
            .content();
    }
}
