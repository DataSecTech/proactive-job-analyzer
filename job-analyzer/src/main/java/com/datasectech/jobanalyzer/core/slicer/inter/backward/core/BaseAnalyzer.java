package com.datasectech.jobanalyzer.core.slicer.inter.backward.core;

import com.datasectech.jobanalyzer.core.checker.common.BaseRuleChecker;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.MethodWrapper;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.UnitContainer;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.Analysis;
import com.datasectech.jobanalyzer.core.slicer.intra.backward.common.MethodCallSiteInfo;
import com.datasectech.jobanalyzer.core.slicer.intra.backward.common.SlicingCriteria;
import com.datasectech.jobanalyzer.core.slicer.intra.backward.method.MethodInfluenceInstructions;
import com.datasectech.jobanalyzer.core.slicer.intra.backward.method.MethodSlicingResult;
import com.datasectech.jobanalyzer.core.slicer.intra.backward.property.PropertyAnalysisResult;
import com.datasectech.jobanalyzer.core.util.FieldInitializationInstructionMap;
import com.datasectech.jobanalyzer.core.util.NamedMethodMap;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseAnalyzer {

    public static final List<String> CRITERIA_CLASSES = new ArrayList<>();

    static {
        CRITERIA_CLASSES.add("java.lang.Class");
        CRITERIA_CLASSES.add("java.lang.reflect.Method");
        CRITERIA_CLASSES.add("java.lang.reflect.Field");
    }

    static void analyzeSliceInternal(String criteriaClass,
                                     List<String> classNames,
                                     String endPoint,
                                     ArrayList<Integer> slicingParameters, BaseRuleChecker checker) {

        SootClass criteriaClazz = Scene.v().getSootClass(criteriaClass);

        if (criteriaClazz.isPhantomClass() || !criteriaClazz.getMethods().toString().contains(endPoint)) {
            return;
        }

        NamedMethodMap.build(classNames);

        NamedMethodMap.addCriteriaClasses(CRITERIA_CLASSES);
        NamedMethodMap.buildCallerCalleeRelation(classNames);

        FieldInitializationInstructionMap.build(classNames);

        runBackwardSlicingAnalysis(NamedMethodMap.getMethod(endPoint),
                slicingParameters, null, null, checker);
    }

    private static void runBackwardSlicingAnalysis(MethodWrapper criteria,
                                                   List<Integer> slicingParams,
                                                   Map<MethodWrapper, List<Analysis>> methodVsAnalysisResult,
                                                   Map<SlicingCriteria, Boolean> slicingCriteriaMap, BaseRuleChecker checker) {

        List<MethodWrapper> callers = criteria.getCallerList();
        if (callers.isEmpty() || slicingParams == null || slicingParams.isEmpty()) {
            return;
        }

        List<MethodCallSiteInfo> callSites = new ArrayList<>();

        for (MethodWrapper caller : callers) {
            for (MethodCallSiteInfo site : caller.getCalleeList()) {
                if (site.getCallee().toString().equals(criteria.toString())) {
                    callSites.add(site);
                }
            }
        }

        for (MethodCallSiteInfo callSiteInfo : callSites) {
            SlicingCriteria slicingCriteria = new SlicingCriteria(callSiteInfo, slicingParams);

            if (methodVsAnalysisResult == null) {
                Map<MethodWrapper, List<Analysis>> newResult = new HashMap<>();
                runBackwardSlicingAnalysisInternal(slicingCriteria, newResult, slicingCriteriaMap, checker);

                for (MethodWrapper methodWrapper : newResult.keySet()) {
                    List<Analysis> analysisList = newResult.get(methodWrapper);
                    for (Analysis analysis : analysisList) {

                        checker.analyzeSlice(analysis);
                    }
                }

                System.gc();
            } else {
                runBackwardSlicingAnalysisInternal(slicingCriteria, methodVsAnalysisResult, slicingCriteriaMap, checker);
            }
        }
    }

    private static void runBackwardSlicingAnalysisInternal(SlicingCriteria slicingCriteria,
                                                           Map<MethodWrapper, List<Analysis>> methodVsAnalysisResult,
                                                           Map<SlicingCriteria, Boolean> slicingCriteriaMap, BaseRuleChecker checker) {

        if (slicingCriteriaMap == null) {
            slicingCriteriaMap = new HashMap<>();
        }

        MethodCallSiteInfo callSiteInfo = slicingCriteria.getMethodCallSiteInfo();
        List<Integer> slicingParams = slicingCriteria.getParameters();

        if (slicingCriteriaMap.get(slicingCriteria) != null) {
            return;
        }

        slicingCriteriaMap.put(slicingCriteria, Boolean.TRUE);

        MethodSlicingResult methodSlicingResult = getInfluencingInstructions(callSiteInfo, slicingParams, callSiteInfo.getCaller().getMethod());

        if (methodSlicingResult.getPropertyUseMap() != null) {
            for (String property : methodSlicingResult.getPropertyUseMap().keySet()) {
                List<PropertyAnalysisResult> propertyAnalysisResults = methodSlicingResult.getPropertyUseMap().get(property);
                for (PropertyAnalysisResult propertyAnalysisResult : propertyAnalysisResults) {

                    List<Analysis> calleeAnalysisList = methodVsAnalysisResult.get(callSiteInfo.getCallee());
                    List<Analysis> callerAnalysisList = methodVsAnalysisResult.get(propertyAnalysisResult.getMethodWrapper());

                    List<Analysis> newAnalysisList = buildNewPropertyAnalysisList(callSiteInfo, methodSlicingResult.getAnalysisResult(),
                            propertyAnalysisResult, calleeAnalysisList);

                    if (callerAnalysisList == null) {
                        callerAnalysisList = new ArrayList<>();
                        methodVsAnalysisResult.put(propertyAnalysisResult.getMethodWrapper(), callerAnalysisList);
                    }

                    callerAnalysisList.addAll(newAnalysisList);

                    runBackwardSlicingAnalysis(propertyAnalysisResult.getMethodWrapper(),
                            propertyAnalysisResult.getInfluencingParams(),
                            methodVsAnalysisResult, slicingCriteriaMap, checker);
                }
            }
        }

        List<Analysis> calleeAnalysisList = methodVsAnalysisResult.get(callSiteInfo.getCallee());
        List<Analysis> callerAnalysisList = methodVsAnalysisResult.get(callSiteInfo.getCaller());

        List<Analysis> newAnalysisList = buildNewAnalysisList(callSiteInfo,
                methodSlicingResult.getAnalysisResult(), calleeAnalysisList);

        if (callerAnalysisList == null) {
            callerAnalysisList = new ArrayList<>();
            methodVsAnalysisResult.put(callSiteInfo.getCaller(), callerAnalysisList);
        }

        callerAnalysisList.addAll(newAnalysisList);

        runBackwardSlicingAnalysis(callSiteInfo.getCaller(),
                methodSlicingResult.getInfluencingParameters(), methodVsAnalysisResult, slicingCriteriaMap, checker);
    }

    private static List<Analysis> buildNewPropertyAnalysisList(MethodCallSiteInfo callSiteInfo,
                                                               List<UnitContainer> methodSlicingResult,
                                                               PropertyAnalysisResult slicingResult,
                                                               List<Analysis> calleeAnalysisList) {

        List<Analysis> newAnalysisList = new ArrayList<>();

        if (calleeAnalysisList != null && !calleeAnalysisList.isEmpty()) {
            for (Analysis analysis : calleeAnalysisList) {
                Analysis newAnalysis = new Analysis();

                StringBuilder newChain = new StringBuilder(callSiteInfo.getCaller().toString().length()
                        + callSiteInfo.getCallee().toString().length()
                        + analysis.getMethodChain().length() + 10);

                newChain.append(callSiteInfo.getCaller())
                        .append("[")
                        .append(callSiteInfo.getLineNumber()).append("]")
                        .append("--->")
                        .append(callSiteInfo.getCallee())
                        .append("--->")
                        .append(analysis.getMethodChain());

                newAnalysis.setMethodChain(newChain.toString());
                newAnalysis.setAnalysisResult(analysis.getAnalysisResult());
                newAnalysis.getAnalysisResult().addAll(methodSlicingResult);
                newAnalysis.getAnalysisResult().addAll(slicingResult.getSlicingResult());

                for (String key : slicingResult.getPropertyUseMap().keySet()) {
                    for (PropertyAnalysisResult res : slicingResult.getPropertyUseMap().get(key))
                        newAnalysis.getAnalysisResult().addAll(res.getSlicingResult());
                }

                newAnalysisList.add(newAnalysis);
            }
        } else {
            Analysis newAnalysis = new Analysis();

            StringBuilder newChain = new StringBuilder();

            newChain.append(callSiteInfo.getCaller())
                    .append("[")
                    .append(callSiteInfo.getLineNumber()).append("]")
                    .append("--->")
                    .append(callSiteInfo.getCallee());

            newAnalysis.setMethodChain(newChain.toString());
            newAnalysis.setAnalysisResult(new ArrayList<UnitContainer>());
            newAnalysis.getAnalysisResult().addAll(methodSlicingResult);
            newAnalysis.getAnalysisResult().addAll(slicingResult.getSlicingResult());

            for (String key : slicingResult.getPropertyUseMap().keySet()) { // TO-DO Recursively add all analysis
                for (PropertyAnalysisResult res : slicingResult.getPropertyUseMap().get(key))
                    newAnalysis.getAnalysisResult().addAll(res.getSlicingResult());
            }

            newAnalysisList.add(newAnalysis);
        }

        return newAnalysisList;
    }

    private static List<Analysis> buildNewAnalysisList(MethodCallSiteInfo callSiteInfo,
                                                       List<UnitContainer> slicingResult,
                                                       List<Analysis> calleeAnalysisList) {
        List<Analysis> newAnalysisList = new ArrayList<>();

        if (calleeAnalysisList != null && !calleeAnalysisList.isEmpty()) {
            for (Analysis analysis : calleeAnalysisList) {
                Analysis newAnalysis = new Analysis();
                StringBuilder newChain = new StringBuilder();

                newChain.append(callSiteInfo.getCaller())
                        .append("[")
                        .append(callSiteInfo.getLineNumber()).append("]")
                        .append("--->")
                        .append(analysis.getMethodChain());

                newAnalysis.setMethodChain(newChain.toString());

                newAnalysis.setAnalysisResult(analysis.getAnalysisResult());
                newAnalysis.getAnalysisResult().addAll(slicingResult);
                newAnalysisList.add(newAnalysis);
            }
        } else {
            Analysis newAnalysis = new Analysis();

            StringBuilder newChain = new StringBuilder();

            newChain.append(callSiteInfo.getCaller())
                    .append("[")
                    .append(callSiteInfo.getLineNumber()).append("]")
                    .append("--->")
                    .append(callSiteInfo.getCallee());

            newAnalysis.setMethodChain(newChain.toString());
            newAnalysis.setAnalysisResult(new ArrayList<UnitContainer>());
            newAnalysis.getAnalysisResult().addAll(slicingResult);
            newAnalysisList.add(newAnalysis);
        }

        return newAnalysisList;
    }

    private static MethodSlicingResult getInfluencingInstructions(MethodCallSiteInfo methodCallSiteInfo,
                                                                  List<Integer> slicingParams,
                                                                  SootMethod m) {
        Body b = m.retrieveActiveBody();

        UnitGraph graph = new ExceptionalUnitGraph(b);
        MethodInfluenceInstructions vbe = new MethodInfluenceInstructions(graph,
                methodCallSiteInfo, slicingParams);

        return vbe.getMethodSlicingResult();
    }
}
