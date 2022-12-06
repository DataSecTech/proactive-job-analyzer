package com.datasectech.jobanalyzer.webservice.services;

import com.datasectech.jobanalyzer.webservice.entities.ProgramAnalysisEntity;
import com.datasectech.jobanalyzer.webservice.repositories.ProgramAnalysisRepository;
import com.datasectech.jobanalyzer.codebuilder.CodeGenerator;
import com.datasectech.jobanalyzer.codebuilder.ProgramAnalysisOutput;
import com.datasectech.jobanalyzer.codebuilder.ScalaProgramAnalyzer;
import com.datasectech.jobanalyzer.codebuilder.ZipBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class ProgramAnalyzerService {
    private static final Logger logger = LoggerFactory.getLogger(ProgramAnalyzerService.class);

    protected ScalaProgramAnalyzer scalaProgramAnalyzer;
    protected ProgramAnalysisRepository programAnalysisRepository;

    @Autowired
    public ProgramAnalyzerService(
            @Value("${secure-dl.job-analyzer.compiler-classpath}") String compilerClasspath,
            ProgramAnalysisRepository programAnalysisRepository
    ) {
        this.scalaProgramAnalyzer = new ScalaProgramAnalyzer(compilerClasspath);
        this.programAnalysisRepository = programAnalysisRepository;
    }

    public ProgramAnalysisOutput analyzeAndUploadProgramInfo(String inputSnippet, String generatedClass, String invocationId) {

        logger.info("[Invocation Id: {}] Starting program analysis", invocationId);
        LocalDateTime startedAt = LocalDateTime.now(Clock.systemUTC());
        ProgramAnalysisOutput analysisOutput = scalaProgramAnalyzer.compileAndAnalyze(generatedClass, invocationId);
        LocalDateTime endedAt = LocalDateTime.now(Clock.systemUTC());

        analysisOutput.startedAt = startedAt;
        analysisOutput.endedAt = endedAt;

        String compiledOutputDir = analysisOutput.compileOutputBase + "/" + CodeGenerator.PACKAGE_NAME;
        String compiledOutputZip = compiledOutputDir + ".zip";

        logger.info("[Invocation Id: {}] Building zip file", invocationId);
        // May consider building the zip in-memory and save directly into Oracle
        ZipBuilder zipBuilder = new ZipBuilder(compiledOutputDir, compiledOutputZip);
        zipBuilder.createZipFile();
        try {
            ProgramAnalysisEntity programAnalysis = new ProgramAnalysisEntity();

            programAnalysis.setInputSnippet(inputSnippet);
            programAnalysis.setGeneratedClass(generatedClass);

            programAnalysis.setCompiledOutput(Files.readAllBytes(Paths.get(compiledOutputZip)));

            ObjectMapper objectMapper = new ObjectMapper();
            String issuesJson = objectMapper.writeValueAsString(analysisOutput.issues);

            // May consider a one-to-many relationship
            programAnalysis.setAnalysisOutput(issuesJson.getBytes(StandardCharsets.UTF_8));

            programAnalysis.setStartedAt(startedAt);
            programAnalysis.setEndedAt(endedAt);

            logger.info("[Invocation Id: {}] Saving result", invocationId);
            // May consider cleaning the generated files and dirs
            ProgramAnalysisEntity programAnalysisSaved = programAnalysisRepository.save(programAnalysis);
            analysisOutput.id = programAnalysisSaved.getId();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return analysisOutput;
    }
}
