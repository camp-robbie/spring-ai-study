package com.study.multimodal.service;

import com.study.multimodal.exception.ContentPolicyViolationException;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.metadata.OpenAiImageGenerationMetadata;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ImageGenerationService {

    private final ImageModel imageModel;

    public ImageGenerationService(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    public Map<String, String> generateImage(String prompt, String quality, String size) {
        int[] dimensions = parseSize(size);
        OpenAiImageOptions options = OpenAiImageOptions.builder()
                .quality(quality != null ? quality : "standard")
                .width(dimensions[0])
                .height(dimensions[1])
                .build();

        ImagePrompt imagePrompt = new ImagePrompt(prompt, options);
        ImageResponse response;
        try {
            response = imageModel.call(imagePrompt);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("content_policy_violation")) {
                throw new ContentPolicyViolationException();
            }
            throw e;
        }

        Map<String, String> result = new HashMap<>();
        if (!response.getResults().isEmpty()) {
            ImageGeneration generation = response.getResult();
            result.put("imageUrl", generation.getOutput().getUrl());

            String revisedPrompt = prompt;
            if (generation.getMetadata() instanceof OpenAiImageGenerationMetadata metadata
                    && metadata.getRevisedPrompt() != null) {
                revisedPrompt = metadata.getRevisedPrompt();
            }
            result.put("revisedPrompt", revisedPrompt);
        }

        return result;
    }

    private int[] parseSize(String size) {
        if (size == null || size.isEmpty()) {
            return new int[]{1024, 1024};
        }

        return switch (size.toLowerCase()) {
            case "1024x1024" -> new int[]{1024, 1024};
            case "1024x1792" -> new int[]{1024, 1792};
            case "1792x1024" -> new int[]{1792, 1024};
            default -> new int[]{1024, 1024};
        };
    }
}
