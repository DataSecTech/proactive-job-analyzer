package com.datasectech.jobanalyzer.core.checker.common;

import com.datasectech.jobanalyzer.core.engine.model.Issue;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.Analysis;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.InvokeUnitContainer;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.UnitContainer;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.AssignInvokeUnitContainer;
import soot.ValueBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PatternMatcherRuleChecker extends BaseRuleChecker {

    private Map<UnitContainer, List<String>> candidates = new HashMap<>();

    @Override
    public void analyzeSlice(Analysis analysis) {

        if (analysis.getAnalysisResult().isEmpty()) {
            return;
        }

        for (UnitContainer e : analysis.getAnalysisResult()) {
            if (checkForMatch(e)) {
                break;
            }
        }
    }

    private boolean checkForMatch(UnitContainer e) {
        if (e instanceof AssignInvokeUnitContainer) {
            List<UnitContainer> resFromInside = ((AssignInvokeUnitContainer) e).getAnalysisResult();

            for (UnitContainer unit : resFromInside) {
                if (checkForMatch(unit)) {
                    return true;
                }
            }
        }

        if (e instanceof InvokeUnitContainer) {
            List<UnitContainer> resFromInside = ((InvokeUnitContainer) e).getAnalysisResult();

            for (UnitContainer unit : resFromInside) {
                if (checkForMatch(unit)) {
                    return true;
                }
            }
        }

        return checkForMatchInternal(e);
    }

    private boolean checkForMatchInternal(UnitContainer e) {

        for (ValueBox usebox : e.getUnit().getUseBoxes()) {

            for (String regex : getPatternsToMatch()) {

                if (usebox.getValue().toString().matches(regex)) {
                    putIntoMap(candidates, e, usebox.getValue().toString());

                    return true;
                }
            }
        }

        return false;
    }

    public List<Issue> generateIssueList() {

        int rule = getRuleId();
        String ruleDesc = getRuleDesc();
        List<Issue> issueList = new ArrayList<>();

        if (!candidates.isEmpty()) {

            for (UnitContainer unitContainer : candidates.keySet()) {
                Issue issue = new Issue();
                issue.setId(rule);
                issue.setDescription(ruleDesc);
                issue.setMethodName(unitContainer.getMethod());
                issue.setLineNumber(unitContainer.getUnit().getJavaSourceStartLineNumber());
                issueList.add(issue);
            }
        }

        candidates.clear();

        return issueList;
    }

    abstract public List<String> getPatternsToMatch();

    abstract public int getRuleId();

    abstract public String getRuleDesc();
}
