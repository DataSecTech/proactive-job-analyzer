package com.datasectech.jobanalyzer.core.checker.core;

import com.datasectech.jobanalyzer.core.checker.common.RuleChecker;
import com.datasectech.jobanalyzer.core.engine.model.AnalysisType;
import com.datasectech.jobanalyzer.core.engine.model.Issue;
import com.datasectech.jobanalyzer.core.util.Utils;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InheritanceChecker implements RuleChecker {

    private static final List<String> RESTRICTED_CLASSES = new ArrayList<>();
    private static final int ISSUE_ID = 5;
    private static final String ISSUE_DESC = "Extending class [%s] is not allowed.";

    static {
        RESTRICTED_CLASSES.add("java.lang.Class");

        // Blacklist command execution
        RESTRICTED_CLASSES.add("java.lang.Runtime");
        RESTRICTED_CLASSES.add("java.lang.ProcessBuilder");

        //  Blacklist URL Connections
        RESTRICTED_CLASSES.add("java.net.URL");
        RESTRICTED_CLASSES.add("java.net.URL");

        //  Blacklist URL Sockets
        RESTRICTED_CLASSES.add("java.net.Socket");
        RESTRICTED_CLASSES.add("java.net.Socket");

        // Blacklist to set security policies and manager
        RESTRICTED_CLASSES.add("java.security.Policy");
        RESTRICTED_CLASSES.add("java.lang.SecurityManager");
    }

    @Override
    public List<Issue> checkRule(AnalysisType type, String projectPath, String projectDependencyPath) throws IOException {
        if (type == AnalysisType.JAR) {
            return analyzeJar(projectPath, projectDependencyPath);

        } else if (type == AnalysisType.CLASS) {
            return analyzeClasses(projectPath, projectDependencyPath);
        }
        return null;
    }

    private List<Issue> analyzeJar(String projectJarPath, String projectDependencyPath) throws IOException {
        String javaHome = Utils.getJavaHome();

        String sootClassPath = Utils.buildSootClassPath(projectJarPath,
                javaHome + "/jre/lib/rt.jar",
                javaHome + "/jre/lib/jce.jar",
                projectDependencyPath
        );

        Options.v().set_keep_line_number(true);
        Options.v().set_allow_phantom_refs(true);

        Scene.v().setSootClassPath(sootClassPath);

        Scene.v().loadBasicClasses();

        List<String> classNames = Utils.getClassNamesFromJarArchive(projectJarPath);
        return getIssueListWithRestrictedClassInheritance(classNames);
    }

    private List<Issue> analyzeClasses(String clazzPath, String clazzDependencyPath) throws IOException {

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

        Scene.v().loadNecessaryClasses();

        return getIssueListWithRestrictedClassInheritance(classNames);
    }

    private static List<Issue> getIssueListWithRestrictedClassInheritance(List<String> classNames) {

        List<Issue> issueList = new ArrayList<>();

        for (String className : classNames) {

            SootClass sClass = Scene.v().loadClassAndSupport(className);

            List<SootClass> superClazzes = getListOfSuperClasses(sClass);

            for (SootClass clazz : superClazzes) {
                for (String clazzName : RESTRICTED_CLASSES) {
                    if (clazz.getName().contains(clazzName)) {
                        Issue issue = new Issue();
                        issue.setId(ISSUE_ID);
                        issue.setDescription(String.format(ISSUE_DESC, className));
                        issueList.add(issue);
                    }
                }
            }
        }

        return issueList;
    }

    private static List<SootClass> getListOfSuperClasses(SootClass sootClass) {
        if (sootClass.hasSuperclass()) {
            SootClass parent = sootClass.getSuperclass();
            List<SootClass> parentSuperClazzes = getListOfSuperClasses(parent);
            parentSuperClazzes.add(parent);
            return parentSuperClazzes;
        } else {
            List<SootClass> classList = new ArrayList<>();
            classList.add(sootClass);
            return classList;
        }
    }
}
