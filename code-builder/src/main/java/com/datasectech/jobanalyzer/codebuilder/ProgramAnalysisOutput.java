package com.datasectech.jobanalyzer.codebuilder;

import com.datasectech.jobanalyzer.core.engine.model.Issue;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

public class ProgramAnalysisOutput {

    public Long id;
    public String javaHome;
    public String systemPath;

    public File projectBase;
    public File compileOutputBase;

    public Integer exitValue;
    public String stdout;
    public String stderr;

    public List<Issue> issues;

    public LocalDateTime startedAt;
    public LocalDateTime endedAt;
}
