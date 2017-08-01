package com.datasectech.jobanalyzer.core.slicer.inter.backward.core;

import com.datasectech.jobanalyzer.core.checker.common.BaseRuleChecker;
import com.datasectech.jobanalyzer.core.util.Utils;
import soot.Scene;
import soot.options.Options;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClassAnalyzer {

    public static void analyzeSlices(String criteriaClass,
                                     String criteriaMethod,
                                     int criteriaParam,
                                     String clazzPath,
                                     String clazzDependencyPath,
                                     BaseRuleChecker checker) throws IOException {

        String javaHome = Utils.getJavaHome();

        List<String> classNames = Utils.getClassNamesFromDir(clazzPath);

        String sootClassPath = Utils.buildSootClassPath(
                javaHome + "/jre/lib/rt.jar",
                javaHome + "/jre/lib/jce.jar",
                clazzDependencyPath
        );

        Options.v().set_keep_line_number(true);
        Options.v().set_allow_phantom_refs(true);

        Scene.v().setSootClassPath(sootClassPath);
        Scene.v().extendSootClassPath(clazzPath);

        String endPoint = "<" + criteriaClass + ": " + criteriaMethod + ">";
        ArrayList<Integer> slicingParameters = new ArrayList<>();
        slicingParameters.add(criteriaParam);

        for (String clazz : BaseAnalyzer.CRITERIA_CLASSES) {
            Scene.v().loadClassAndSupport(clazz);
        }

        for (String clazz : classNames) {
            Scene.v().loadClassAndSupport(clazz);
        }

        Scene.v().loadNecessaryClasses();

        BaseAnalyzer.analyzeSliceInternal(criteriaClass, classNames, endPoint, slicingParameters, checker);
    }
}
