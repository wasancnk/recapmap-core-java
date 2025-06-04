package com.recapmap.core.controller;

import com.recapmap.core.service.OpenAiVisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenAiVisionTestController {
    @Autowired
    private OpenAiVisionService openAiVisionService;

    @GetMapping("/api/test-vision-hello")
    public String helloWorldVisionTest() {
        return openAiVisionService.helloWorldTest();
    }
}
