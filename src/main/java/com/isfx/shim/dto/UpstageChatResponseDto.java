package com.isfx.shim.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class UpstageChatResponseDto {
    
    @JsonProperty("choices")
    private List<Choice> choices;
    
    @Getter
    public static class Choice {
        @JsonProperty("message")
        private Message message;
    }
    
    @Getter
    public static class Message {
        @JsonProperty("content")
        private String content;
    }
    
    public String getContent() {
        if (choices != null && !choices.isEmpty() && choices.get(0).message != null) {
            return choices.get(0).message.content;
        }
        return null;
    }
}

