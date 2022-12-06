package com.datasectech.jobanalyzer.webservice.controllers;

import com.datasectech.jobanalyzer.webservice.dto.InputSnippet;
import com.datasectech.jobanalyzer.webservice.services.ProgramAnalyzerService;
import com.datasectech.jobanalyzer.codebuilder.CodeGenerator;
import com.datasectech.jobanalyzer.codebuilder.ProgramAnalysisOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProgramAnalysisController {
    public static final Logger logger = LoggerFactory.getLogger(ProgramAnalysisController.class);

    protected ProgramAnalyzerService programAnalyzerService;

    public ProgramAnalysisController(ProgramAnalyzerService programAnalyzerService) {
        this.programAnalyzerService = programAnalyzerService;
    }

    @GetMapping("/")
    public String index() {
        return "Hello from SecureDL Proactive program analyzer";
    }

    @PostMapping("/program-analyzer/analyze-json")
    public ProgramAnalysisOutput analyze(@RequestBody InputSnippet inputSnippet) {
        return analyzeCode(inputSnippet.getCodeSnippet());
    }

    @PostMapping("/program-analyzer/analyze")
    public ProgramAnalysisOutput analyze(@RequestParam String codeSnippet) {
        return analyzeCode(codeSnippet);
    }

    protected ProgramAnalysisOutput analyzeCode(String codeSnippet) {
        String invocationId = CodeGenerator.generateRandomInvocationId();
        String generatedClass = CodeGenerator.generateClass(codeSnippet, invocationId);

        logger.info("Starting analysis with invocation id: {}", invocationId);

        return programAnalyzerService.analyzeAndUploadProgramInfo(codeSnippet, generatedClass, invocationId);
    }
}
