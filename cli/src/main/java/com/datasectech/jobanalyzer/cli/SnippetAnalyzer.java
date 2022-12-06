package com.datasectech.jobanalyzer.cli;

import com.datasectech.jobanalyzer.codebuilder.CodeGenerator;
import com.datasectech.jobanalyzer.codebuilder.ProgramAnalysisOutput;
import com.datasectech.jobanalyzer.codebuilder.ScalaProgramAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

@Command(name = "snippet")
public class SnippetAnalyzer implements Callable<Integer> {

    private final static Logger logger = LogManager.getLogger(SnippetAnalyzer.class);

    @Option(names = {"-c", "--compilerClasspath"}, description = "Compiler classpath path with dependencies", defaultValue = "")
    private String compilerClasspath;

    @Option(names = {"-s", "--snippetPath"}, description = "Snippet path", defaultValue = "")
    private String snippetPath;

    @Override
    public Integer call() throws Exception {

        logger.debug("SnippetPath: {}", snippetPath);
        logger.debug("CompilerClasspath: {}", compilerClasspath);

        ScalaProgramAnalyzer scalaProgramAnalyzer = new ScalaProgramAnalyzer(compilerClasspath);

        String invocationId = CodeGenerator.generateRandomInvocationId();
        String snippet = new String(Files.readAllBytes(Paths.get(snippetPath)), StandardCharsets.UTF_8);

        String generatedClass = CodeGenerator.generateClass(snippet, invocationId);

        logger.info("[Invocation Id: {}] Starting program analysis", invocationId);
        LocalDateTime startedAt = LocalDateTime.now(Clock.systemUTC());

        ProgramAnalysisOutput analysisOutput = scalaProgramAnalyzer.compileAndAnalyze(generatedClass, invocationId);
        LocalDateTime endedAt = LocalDateTime.now(Clock.systemUTC());

        Duration duration = Duration.between(startedAt, endedAt);
        logger.info("[Invocation Id: {}] Analysis finished; time required: {} sec.", invocationId, duration.getSeconds());

        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        logger.info("Analysis output: -\n{}", objectMapper.writeValueAsString(analysisOutput));

        return 0;
    }
}
