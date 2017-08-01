package com.datasectech.jobanalyzer.core.checker.core;

import com.datasectech.jobanalyzer.core.checker.common.RuleChecker;
import com.datasectech.jobanalyzer.core.engine.model.AnalysisType;
import com.datasectech.jobanalyzer.core.engine.model.Issue;
import com.datasectech.jobanalyzer.core.util.Utils;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.options.Options;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RestrictedMethodInvocationChecker implements RuleChecker {

    private static final List<String> RESTRICTED_METHOD_SIGNATURE = new ArrayList<>();
    private static final int ISSUE_ID = 4;
    private static final String ISSUE_DESC = "Invoking [%s] is not allowed.";

    static {
        RESTRICTED_METHOD_SIGNATURE.add("java.lang.Class: java.lang.Class forName");

        // Blacklist command execution
        RESTRICTED_METHOD_SIGNATURE.add("java.lang.Runtime: java.lang.Process exec");
        RESTRICTED_METHOD_SIGNATURE.add("java.lang.ProcessBuilder: void <init>");

        //  Blacklist URL Connections
        RESTRICTED_METHOD_SIGNATURE.add("java.net.URL: java.net.URLConnection openConnection");
        RESTRICTED_METHOD_SIGNATURE.add("java.net.URL: void <init>");

        //  Blacklist URL Sockets
        RESTRICTED_METHOD_SIGNATURE.add("java.net.Socket: void <init>");
        RESTRICTED_METHOD_SIGNATURE.add("java.net.Socket: void connect");

        // Blacklist to set security policies and manager
        RESTRICTED_METHOD_SIGNATURE.add("java.security.Policy: void setPolicy");
        RESTRICTED_METHOD_SIGNATURE.add("java.lang.System: void setSecurityManager");
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
                projectDependencyPath);

        Options.v().set_keep_line_number(true);
        Options.v().set_allow_phantom_refs(true);

        Scene.v().setSootClassPath(sootClassPath);

        Scene.v().loadBasicClasses();

        List<String> classNames = Utils.getClassNamesFromJarArchive(projectJarPath);
        return getIssueListWithRestricted(classNames);
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

        return getIssueListWithRestricted(classNames);
    }

    private static List<Issue> getIssueListWithRestricted(List<String> classNames) {

        List<Issue> issueList = new ArrayList<>();

        for (String className : classNames) {

            SootClass sClass = Scene.v().loadClassAndSupport(className);

            for (SootMethod method : sClass.getMethods()) {

                if (method.isConcrete()) {
                    Body b = method.retrieveActiveBody();
                    DirectedGraph g = new ExceptionalUnitGraph(b);

                    for (Object unit : g) {
                        if (unit instanceof JInvokeStmt) {

                            for (String methodSig : RESTRICTED_METHOD_SIGNATURE) {

                                if (((JInvokeStmt) unit).getInvokeExprBox().toString().contains(methodSig)) {
                                    Issue issue = new Issue();
                                    issue.setLineNumber(((JInvokeStmt) unit).getJavaSourceStartLineNumber());
                                    issue.setMethodName(method.getName());
                                    issue.setId(ISSUE_ID);
                                    issue.setDescription(String.format(ISSUE_DESC, methodSig));
                                    issueList.add(issue);
                                }
                            }


                        } else if (unit instanceof JAssignStmt && ((JAssignStmt) unit).containsInvokeExpr()) {

                            for (String methodSig : RESTRICTED_METHOD_SIGNATURE) {

                                if (((JAssignStmt) unit).getInvokeExprBox().toString().contains(methodSig)) {
                                    Issue issue = new Issue();
                                    issue.setLineNumber(((JAssignStmt) unit).getJavaSourceStartLineNumber());
                                    issue.setMethodName(method.getName());
                                    issue.setId(ISSUE_ID);
                                    issue.setDescription(String.format(ISSUE_DESC, methodSig));
                                    issueList.add(issue);
                                }
                            }
                        }
                    }
                }
            }
        }

        return issueList;
    }
}
