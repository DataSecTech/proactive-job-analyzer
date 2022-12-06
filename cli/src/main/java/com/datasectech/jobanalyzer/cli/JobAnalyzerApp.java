package com.datasectech.jobanalyzer.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "SDLJobAnalyzer",
        mixinStandardHelpOptions = true,
        description = "SecureDL Proactive Job Analyzer for jar and scala snippet",
        subcommands = {
                SnippetAnalyzer.class,
                JarAnalyzer.class
        }
)
public class JobAnalyzerApp {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new JobAnalyzerApp())
                .execute(args);
        System.exit(exitCode);
    }
}
