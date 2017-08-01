package com.datasectech.jobanalyzer.core.checker.common;

import com.datasectech.jobanalyzer.core.engine.model.AnalysisType;
import com.datasectech.jobanalyzer.core.engine.model.Issue;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.Analysis;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.UnitContainer;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.core.ClassAnalyzer;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.core.JarAnalyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseRuleChecker implements RuleChecker {

    @Override
    public List<Issue> checkRule(AnalysisType type, String projectPath, String projectDependencyPath) throws IOException {

        for (Criteria criteria : getCriteriaList()) {

            if (type == AnalysisType.JAR) {
                JarAnalyzer.analyzeSlices(criteria.getClassName(),
                        criteria.getMethodName(),
                        criteria.getParam(),
                        projectPath,
                        projectDependencyPath, this);
            } else if (type == AnalysisType.CLASS) {
                ClassAnalyzer.analyzeSlices(criteria.getClassName(),
                        criteria.getMethodName(),
                        criteria.getParam(),
                        projectPath,
                        projectDependencyPath, this);
            }
        }

        return generateIssueList();
    }

    public abstract List<Criteria> getCriteriaList();

    public abstract void analyzeSlice(Analysis analysis);

    public abstract List<Issue> generateIssueList();

    void putIntoMap(Map<UnitContainer, List<String>> unitStringMap, UnitContainer e, String value) {

        List<String> values = unitStringMap.get(e);
        if (values == null) {
            values = new ArrayList<>();
            values.add(value);
            unitStringMap.put(e, values);
            return;
        }

        if (!values.toString().contains(value)) {
            values.add(value);
        }
    }
}
