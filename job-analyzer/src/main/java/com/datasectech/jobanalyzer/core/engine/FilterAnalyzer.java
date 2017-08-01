package com.datasectech.jobanalyzer.core.engine;

import com.datasectech.jobanalyzer.core.checker.common.RuleChecker;
import com.datasectech.jobanalyzer.core.checker.core.*;
import com.datasectech.jobanalyzer.core.engine.model.AnalysisType;
import com.datasectech.jobanalyzer.core.engine.model.Issue;
import com.datasectech.jobanalyzer.core.util.NamedMethodMap;
import com.datasectech.jobanalyzer.core.util.Utils;
import com.datasectech.jobanalyzer.core.util.FieldInitializationInstructionMap;
import soot.G;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FilterAnalyzer implements Closeable {

    private static List<RuleChecker> ruleCheckerList = new ArrayList<>();

    static {

        ruleCheckerList.add(new ReflectionMethodChecker());
        ruleCheckerList.add(new ReflectionFieldChecker());
        ruleCheckerList.add(new PackageNameChecker());
        ruleCheckerList.add(new RestrictedMethodInvocationChecker());
        ruleCheckerList.add(new InheritanceChecker());
    }

    protected String previousJavaHome;

    public FilterAnalyzer() {
        Utils.setJavaHome(systemJavaHome());
    }

    public FilterAnalyzer(String javaHome) {
        previousJavaHome = systemJavaHome();
        Utils.setJavaHome(javaHome);
    }

    public String systemJavaHome() {
        return System.getenv("JAVA_HOME");
    }

    public List<Issue> getAnalysisResult(AnalysisType type, String projectJarPath, String projectDependencyPath, int depth) throws IOException {
        return getAnalysisResult(type, projectJarPath, projectDependencyPath, depth, 0);
    }

    public List<Issue> getAnalysisResult(AnalysisType type, String projectJarPath, String projectDependencyPath, int depth, int offset) throws IOException {

        List<Issue> issues = new ArrayList<>();

        Utils.initDepth(depth);

        for (RuleChecker ruleChecker : ruleCheckerList) {

            List<Issue> issuesFromRule;

            issuesFromRule = ruleChecker.checkRule(type, projectJarPath, projectDependencyPath);
            if (issuesFromRule != null) {
                issues.addAll(issuesFromRule);
            }
        }

        NamedMethodMap.clearCallerCalleeGraph();
        FieldInitializationInstructionMap.clearFields();
        G.reset();

        if (offset > 0) {
            // Remove the offset number of lines from the issues line numbers
            // Necessary for zeppelin submitted snippet

            for (Issue issue : issues) {
                issue.setLineNumber(issue.getLineNumber() - offset);
            }
        }

        return issues;
    }

    @Override
    public void close() throws IOException {
        if (previousJavaHome == null || previousJavaHome.isEmpty()) {
            return;
        }

        Utils.setJavaHome(previousJavaHome);
    }
}
