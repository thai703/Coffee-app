package com.example.myapplication.model.gemini;

import java.util.Collections;
import java.util.List;

public class GeminiRequest {
    private List<Content> contents;

    public GeminiRequest(String text) {
        this.contents = Collections.singletonList(new Content(new Part(text)));
    }

    public static class Content {
        private List<Part> parts;
        private String role;

        public Content(Part part) {
            this.parts = Collections.singletonList(part);
            this.role = "user";
        }
    }

    public static class Part {
        private String text;

        public Part(String text) {
            this.text = text;
        }
    }
}
