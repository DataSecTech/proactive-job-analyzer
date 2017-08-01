package com.datasectech.jobanalyzer.core.checker.core;

import com.datasectech.jobanalyzer.core.checker.common.Criteria;
import com.datasectech.jobanalyzer.core.checker.common.PatternMatcherRuleChecker;

import java.util.ArrayList;
import java.util.List;

public class ReflectionMethodChecker extends PatternMatcherRuleChecker {

    private static final List<String> MATCHING_CRITERIA = new ArrayList<>();
    private static final List<Criteria> CRITERIA_LIST = new ArrayList<>();

    static {
        MATCHING_CRITERIA.add("(.)*org\\.apache\\.spark\\.SparkContext(.)*");

        Criteria criteria = new Criteria();
        criteria.setClassName("java.lang.reflect.Method");
        criteria.setMethodName("java.lang.Object invoke(java.lang.Object,java.lang.Object[])");
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
        return 1;
    }

    @Override
    public String getRuleDesc() {
        return "Found method invocation using reflection";
    }
}
