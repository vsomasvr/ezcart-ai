package com.example.ezcart.ai.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @PostMapping("/execute")
    public Map<String, String> execute(@RequestBody String message, Authentication authentication) {
        return Collections.singletonMap("text", "Thanks for your message! I'm a placeholder AI and I'm processing your request.");
    }
}
