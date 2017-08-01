package com.datasectech.jobanalyzer.core.checker.core;

import com.datasectech.jobanalyzer.core.checker.common.Criteria;
import com.datasectech.jobanalyzer.core.checker.common.PatternMatcherRuleChecker;

import java.util.ArrayList;
import java.util.List;

public class ReflectionFieldChecker extends PatternMatcherRuleChecker {

    private static final List<String> MATCHING_CRITERIA = new ArrayList<>();
    private static final List<Criteria> CRITERIA_LIST = new ArrayList<>();

    static {
        MATCHING_CRITERIA.add("(.)*org\\.apache\\.spark\\.SparkContext(.)*");

        Criteria criteria = new Criteria();
        criteria.setClassName("java.lang.reflect.Field");
        criteria.setMethodName("java.lang.Object get(java.lang.Object)");
        criteria.setParam(0);

        CRITERIA_LIST.add(criteria);
    }

    @Override
    public List<Criteria> getCriteriaList() {
        return CRITERIA_LIST;
    }

    @Override
    public List<String> getPatternsToMatch() {
        return MATCHING_CRITERIA;
    }

    @Override
    public int getRuleId() {
        return 2;
    }

    @Override
    public String getRuleDesc() {
        return "Found field access using reflection";
    }
}
