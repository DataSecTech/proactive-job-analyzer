package com.datasectech.jobanalyzer.codebuilder;

import com.datasectech.jobanalyzer.codebuilder.exceptions.ProgramAnalysisException;
import com.datasectech.jobanalyzer.core.engine.FilterAnalyzer;
import com.datasectech.jobanalyzer.core.engine.model.AnalysisType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class ScalaProgramAnalyzer {
    private static final Logger logger = LogManager.getLogger(ScalaProgramAnalyzer.class);

    protected String compilerClasspath;

    public ScalaProgramAnalyzer(String compilerClasspath) {
        this.compilerClasspath = compilerClasspath;
    }

    public void writeCode(File path, String code) {

        try (PrintWriter pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream(path)))) {
            pw.print(code);
        } catch (FileNotFoundException e) {
            throw new ProgramAnalysisException("Failed to write generated code");
        }
    }

    // TODO handle single compilation error, with detail information printing..
    public ProgramAnalysisOutput compileAndAnalyze(String code, String invocationId) {
        try {
            logger.debug("[Invocation id: {}] Generating code", invocationId);
            File projectBase = new File("/tmp", "snippet_" + invocationId);
            File outputDir = new File(projectBase, "output");

            if (!outputDir.mkdirs()) {
                throw new RuntimeException("Failed to create output directory");
            }

            File inputSnippetFile = new File(projectBase, "Snippet.scala");
            writeCode(inputSnippetFile, code);

            String path = System.getenv("PATH");
            String jdkJavaHome = System.getenv("JAVA_HOME");

            logger.debug("[Invocation id: {}] Compiling code", invocationId);
            Process process = Runtime.getRuntime().exec(
                    "scalac " + inputSnippetFile +
                            " -d " + outputDir +
                            " -classpath " + compilerClasspath,
                    new String[]{"JAVA_HOME=" + jdkJavaHome, "PATH=" + path}, projectBase
            );

            StringBuilder output = new StringBuilder();
            StringBuilder outputErr = new StringBuilder();
            BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = outReader.readLine()) != null) {
                output.append(line).append("\n");
            }

            while ((line = errReader.readLine()) != null) {
                outputErr.append(line).append("\n");
            }

            ProgramAnalysisOutput analysisOutput = new ProgramAnalysisOutput();

            analysisOutput.javaHome = jdkJavaHome;
            analysisOutput.systemPath = path;
            analysisOutput.projectBase = projectBase;
            analysisOutput.compileOutputBase = outputDir;

            int exitVal = process.waitFor();

            analysisOutput.exitValue = exitVal;
            analysisOutput.stdout = output.toString();
            analysisOutput.stderr = outputErr.toString();

            if (exitVal != 0) {
                String message = "Snippet compilation failed with exit code " + exitVal;
                throw new ProgramAnalysisException(analysisOutput, message);
            }

            try (FilterAnalyzer fa = new FilterAnalyzer(jdkJavaHome)) {

                analysisOutput.issues = fa.getAnalysisResult(
                        AnalysisType.CLASS, outputDir.toString(), "", 1, 18
                );

                return analysisOutput;
            }

        } catch (IOException | InterruptedException e) {
            throw new ProgramAnalysisException("Error occurred in code generation and compilation - " + e.getMessage());
        }
    }
}
