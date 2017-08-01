package com.datasectech.jobanalyzer.core.checker.common;

import com.datasectech.jobanalyzer.core.engine.model.AnalysisType;
import com.datasectech.jobanalyzer.core.engine.model.Issue;

import java.io.IOException;
import java.util.List;

public interface RuleChecker {

    List<Issue> checkRule(AnalysisType type, String projectPath, String projectDependencyPath) throws IOException;
}
