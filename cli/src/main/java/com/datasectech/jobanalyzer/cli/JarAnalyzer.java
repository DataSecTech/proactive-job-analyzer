package com.datasectech.jobanalyzer.cli;

import com.datasectech.jobanalyzer.core.checker.common.RuleChecker;
import com.datasectech.jobanalyzer.core.checker.core.*;
import com.datasectech.jobanalyzer.core.engine.model.AnalysisType;
import com.datasectech.jobanalyzer.core.engine.model.Issue;
import com.datasectech.jobanalyzer.core.util.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "jar")
public class JarAnalyzer implements Callable<Integer> {

    private final static Logger logger = LogManager.getLogger(JarAnalyzer.class);

    private static final List<RuleChecker> ruleCheckerList = new ArrayList<>();

    static {
        ruleCheckerList.add(new ReflectionMethodChecker());
        ruleCheckerList.add(new ReflectionFieldChecker());
        ruleCheckerList.add(new PackageNameChecker());
        ruleCheckerList.add(new RestrictedMethodInvocationChecker());
        ruleCheckerList.add(new InheritanceChecker());
    }

    @Option(names = {"-j", "--jarPath"}, description = "Jar to analyze", required = true)
    private File jarPath;

    @Option(names = {"-p", "--dependencyPath"}, description = "Dependency path", defaultValue = "")
    private File dependencyPath;

    @Option(names = {"-d", "--depth"}, description = "Analysis depth", defaultValue = "1")
    private Integer depth;

    @Option(names = {"-o", "--output"}, description = "Save output as json into files")
    private File outputPath;

    public JarAnalyzer() {
    }

    @Override
    public Integer call() throws Exception {

        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null || javaHome.isEmpty()) {
            throw new RuntimeException("JAVA_HOME environment variable is not set");
        }

        Utils.setJavaHome(javaHome);
        Utils.initDepth(depth);

        if (!jarPath.exists()) {
            throw new RuntimeException("Jar file not found");
        }

        List<Issue> issues = new ArrayList<>();
        for (RuleChecker ruleChecker : ruleCheckerList) {
            logger.info("Checking with rule " + ruleChecker.getClass());

            List<Issue> fromAnalysis = ruleChecker.checkRule(AnalysisType.JAR, jarPath.toString(), dependencyPath.toString());

            if (fromAnalysis != null) {
                issues.addAll(fromAnalysis);
            }
        }

        logger.info("Found issues: {}", issues.size());

        if (outputPath != null) {

            logger.info("Writing issues: {}", outputPath.toString());

            ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

            try (Writer writer = new BufferedWriter(new FileWriter(outputPath))) {
                writer.write(objectMapper.writeValueAsString(issues));
            }
        } else {
            logger.info(issues);
        }

        return 0;
    }
}
