package com.datasectech.jobanalyzer.core.slicer.inter.backward.core;

import com.datasectech.jobanalyzer.core.checker.common.BaseRuleChecker;
import com.datasectech.jobanalyzer.core.util.Utils;
import soot.Scene;
import soot.options.Options;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JarAnalyzer {

    public static void analyzeSlices(String criteriaClass,
                                     String criteriaMethod,
                                     int criteriaParam,
                                     String projectJarPath,
                                     String projectDependencyPath, BaseRuleChecker checker) throws IOException {

        String javaHome = Utils.getJavaHome();
        List<String> classNames = Utils.getClassNamesFromJarArchive(projectJarPath);

        for (String dependency : Utils.getJarsInDirectory(projectDependencyPath)) {
            classNames.addAll(Utils.getClassNamesFromJarArchive(dependency));
        }

        String sootClassPath = Utils.buildSootClassPath(projectJarPath,
                javaHome + "/jre/lib/rt.jar",
                javaHome + "/jre/lib/jce.jar",
                projectDependencyPath
        );

        Options.v().set_keep_line_number(true);
        Options.v().set_allow_phantom_refs(true);

        Scene.v().setSootClassPath(sootClassPath);

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
