package com.example.ezcart.ai.controller;

import java.util.Map;

public interface Agent {
    Map<String, String> execute(String message, String principal);
}
