package com.datasectech.jobanalyzer.core.checker.core;

import com.datasectech.jobanalyzer.core.checker.common.RuleChecker;
import com.datasectech.jobanalyzer.core.engine.model.AnalysisType;
import com.datasectech.jobanalyzer.core.engine.model.Issue;
import com.datasectech.jobanalyzer.core.util.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PackageNameChecker implements RuleChecker {

    private static final List<String> RESTRICTED_PACKAGE_NAMES = new ArrayList<>();
    private static final int ISSUE_ID = 3;
    private static final String ISSUE_DESC = "Declaring package name [%s] is not allowed.";

    static {
        RESTRICTED_PACKAGE_NAMES.add("org.apache.spark");
    }

    @Override
    public List<Issue> checkRule(AnalysisType type, String projectPath, String projectDependencyPath) throws IOException {

        if (type == AnalysisType.JAR) {
            List<String> classNames = Utils.getClassNamesFromJarArchive(projectPath);
            return checkForPackageNames(classNames);

        } else if (type == AnalysisType.CLASS) {
            List<String> classNames = Utils.getClassNamesFromDir(projectPath);
            return checkForPackageNames(classNames);
        }

        return null;
    }

    private List<Issue> checkForPackageNames(List<String> classNames) {
        List<Issue> issues = new ArrayList<>();

        for (String className : classNames) {
            for (String packageName : RESTRICTED_PACKAGE_NAMES) {
                if (className.contains(packageName)) {
                    Issue issue = new Issue();
                    issue.setId(ISSUE_ID);
                    issue.setDescription(String.format(ISSUE_DESC, packageName));
                    issues.add(issue);
                }
            }
        }

        return issues;
    }

}
