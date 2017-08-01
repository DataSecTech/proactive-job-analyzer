package com.datasectech.jobanalyzer.core.util;

import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.InvokeUnitContainer;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.UnitContainer;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.AssignInvokeUnitContainer;
import com.datasectech.jobanalyzer.core.slicer.intra.backward.heuristic.HeuristicBasedAnalysisResult;
import com.datasectech.jobanalyzer.core.slicer.intra.backward.heuristic.HeuristicBasedInstructions;
import com.datasectech.jobanalyzer.core.slicer.intra.backward.orthogonal.OrthogonalInfluenceInstructions;
import com.datasectech.jobanalyzer.core.slicer.intra.backward.orthogonal.OrthogonalSlicingResult;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.util.Chain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static soot.SootClass.BODIES;

public class Utils {


    public static int DEPTH = 0;

    public static void initDepth(int depth) {
        DEPTH = depth;
    }

    public static List<String> getClassNamesFromJarArchive(String jarPath) throws IOException {
        List<String> classNames = new ArrayList<>();
        ZipInputStream zip = new ZipInputStream(new FileInputStream(jarPath));
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                String className = entry.getName().replace('/', '.');
                classNames.add(className.substring(0, className.length() - ".class".length()));
            }
        }
        return classNames;
    }

    public static String buildSootClassPath(String... paths) {
        return buildSootClassPath(Arrays.asList(paths));
    }

    public static String buildSootClassPath(List<String> paths) {

        StringBuilder classPath = new StringBuilder();

        for (String path : paths) {

            if (path.endsWith(".jar")) {
                classPath.append(path);
                classPath.append(":");
            } else {
                File dir = new File(path);

                if (dir.isDirectory()) {
                    File[] files = dir.listFiles();

                    if (files == null) {
                        continue;
                    }

                    for (File file : files) {
                        if (file.getName().endsWith(".jar")) {
                            classPath.append(file.getAbsolutePath());
                            classPath.append(":");
                        }
                    }
                }
            }
        }

        return classPath.toString();
    }

    public static List<String> getJarsInDirectory(String path) {

        List<String> jarFiles = new ArrayList<>();
        File dir = new File(path);

        if (dir.isDirectory()) {
            File[] files = dir.listFiles();

            if (files == null) {
                return jarFiles;
            }

            for (File file : files) {
                if (file.getName().endsWith(".jar")) {
                    jarFiles.add(file.getAbsolutePath());
                }
            }
        }

        return jarFiles;
    }

    public static Map<String, List<SootClass>> getClassHierarchyAnalysis(List<String> classNames) {

        Map<String, List<SootClass>> classHierarchyMap = new HashMap<>();

        for (String className : classNames) {

            SootClass sClass = Scene.v().getSootClass(className);
            Chain<SootClass> parents = sClass.getInterfaces();

            if (sClass.hasSuperclass()) {
                SootClass superClass = sClass.getSuperclass();

                List<SootClass> childList = classHierarchyMap.get(superClass.getName());

                if (childList == null) {
                    childList = new ArrayList<>();
                    classHierarchyMap.put(superClass.getName(), childList);
                }

                if (childList.isEmpty()) {
                    childList.add(superClass);
                }
                childList.add(sClass);
            }

            for (SootClass parent : parents) {
                List<SootClass> childList = classHierarchyMap.get(parent.getName());

                if (childList == null) {
                    childList = new ArrayList<>();
                    classHierarchyMap.put(parent.getName(), childList);
                }

                if (childList.isEmpty()) {
                    childList.add(parent);
                }
                childList.add(sClass);
            }
        }

        return classHierarchyMap;
    }

    public static List<Integer> findInfluencingParamters(List<UnitContainer> analysisResult) {
        List<Integer> influencingParam = new ArrayList<>();

        for (int index = analysisResult.size() - 1; index >= 0; index--) {
            UnitContainer unit = analysisResult.get(index);

            for (ValueBox useBox : unit.getUnit().getUseBoxes()) {
                String useboxStr = useBox.getValue().toString();
                if (useboxStr.contains("@parameter")) {
                    Integer param = Integer.valueOf(useboxStr.substring("@parameter".length(), useboxStr.indexOf(':')));
                    influencingParam.add(param);
                }
            }
        }

        return influencingParam;
    }

    private static final List<String> ASSIGN_DONT_VISIT = new ArrayList<>();
    private static final List<String> INVOKE_DONT_VISIT = new ArrayList<>();

    static {
        ASSIGN_DONT_VISIT.add("<java.util.Map: java.lang.Object get(java.lang.Object)>");
        INVOKE_DONT_VISIT.add("<java.util.Map: java.lang.Object put(java.lang.Object,java.lang.Object)>");
        INVOKE_DONT_VISIT.add("java.lang.String: void <init>");
    }

    public static UnitContainer createAssignInvokeUnitContainer(Unit currInstruction, String caller, int depth) {

        for (String dontVisit : ASSIGN_DONT_VISIT) {
            if (currInstruction.toString().contains(dontVisit)) {
                UnitContainer unitContainer = new UnitContainer();
                unitContainer.setUnit(currInstruction);
                unitContainer.setMethod(caller);
                return unitContainer;
            }
        }

        AssignInvokeUnitContainer unitContainer = new AssignInvokeUnitContainer();

        SootMethod method = ((JAssignStmt) currInstruction).getInvokeExpr().getMethod();
        if (method != null && method.isConcrete()) {

            Scene.v().forceResolve(method.getDeclaringClass().getName(), BODIES);

            List<UnitContainer> intraAnalysis = null;

            if (depth == 1) {

                HeuristicBasedInstructions returnInfluencingInstructions = new HeuristicBasedInstructions(method,
                        "return");

                intraAnalysis = returnInfluencingInstructions.getAnalysisResult().getAnalysis();
            } else {

                OrthogonalInfluenceInstructions other = new OrthogonalInfluenceInstructions(method, "return", depth - 1);
                intraAnalysis = other.getOrthogonalSlicingResult().getAnalysisResult();
            }

            // Get args
            List<Integer> args = Utils.findInfluencingParamters(intraAnalysis);

            // Get fields
            Set<String> usedFields = new HashSet<>();
            for (UnitContainer iUnit : intraAnalysis) {
                for (ValueBox usebox : iUnit.getUnit().getUseBoxes()) {
                    if (usebox.getValue().toString().startsWith("r0.") || usebox.getValue().toString().startsWith("this.")) {
                        usedFields.add(usebox.getValue().toString());
                    }
                }
            }

            unitContainer.setArgs(args);
            unitContainer.setAnalysisResult(intraAnalysis);
            unitContainer.setMethod(caller);
            unitContainer.setProperties(usedFields);
        }

        return unitContainer;
    }

    public static int isArgOfAssignInvoke(ValueBox useBox, Unit unit) {

        if (unit instanceof JAssignStmt && unit.toString().contains("invoke ")) {

            InvokeExpr invokeExpr = ((JAssignStmt) unit).getInvokeExpr();
            List<Value> args = invokeExpr.getArgs();
            for (int index = 0; index < args.size(); index++) {
                if (args.get(index).equivTo(useBox.getValue())) {
                    return index;
                }
            }
        }

        return -1;
    }

    public static int isArgOfInvoke(ValueBox useBox, Unit unit) {

        if (unit instanceof JInvokeStmt) {

            InvokeExpr invokeExpr = ((JInvokeStmt) unit).getInvokeExpr();
            List<Value> args = invokeExpr.getArgs();
            for (int index = 0; index < args.size(); index++) {
                if (args.get(index).equivTo(useBox.getValue())) {
                    return index;
                }
            }
        }

        return -1;
    }

    public static boolean isArgOfByteArrayCreation(ValueBox useBox, Unit unit) {
        if (unit.toString().contains(" newarray ")) {
            for (ValueBox valueBox : unit.getUseBoxes()) {
                if (valueBox.getValue().equivTo(useBox.getValue())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static UnitContainer createInvokeUnitContainer(Unit currInstruction, String caller, List<String> usedFields, int depth) {

        for (String dontVisit : INVOKE_DONT_VISIT) {
            if (currInstruction.toString().contains(dontVisit)) {
                UnitContainer unitContainer = new UnitContainer();
                unitContainer.setUnit(currInstruction);
                unitContainer.setMethod(caller);
                return unitContainer;
            }
        }

        InvokeUnitContainer unitContainer = new InvokeUnitContainer();
        SootMethod method = ((JInvokeStmt) currInstruction).getInvokeExpr().getMethod();

        if (method.isConcrete()) {

            Scene.v().forceResolve(method.getDeclaringClass().getName(), BODIES);

            if (depth == 1) {

                for (String field : usedFields) {

                    HeuristicBasedInstructions influencingInstructions = new HeuristicBasedInstructions(method, field);
                    HeuristicBasedAnalysisResult propAnalysis = influencingInstructions.getAnalysisResult();

                    if (propAnalysis.getAnalysis() != null) {
                        // Get args
                        List<Integer> args = Utils.findInfluencingParamters(propAnalysis.getAnalysis());
                        unitContainer.setArgs(args);
                        unitContainer.setMethod(caller);
                        unitContainer.getDefinedFields().add(field);
                        unitContainer.setAnalysisResult(propAnalysis.getAnalysis());
                    }
                }
            } else {

                for (String field : usedFields) {

                    OrthogonalInfluenceInstructions other = new OrthogonalInfluenceInstructions(method, field, depth - 1);
                    OrthogonalSlicingResult orthoAnalysis = other.getOrthogonalSlicingResult();

                    if (orthoAnalysis.getAnalysisResult() != null) {
                        // Get args
                        List<Integer> args = Utils.findInfluencingParamters(orthoAnalysis.getAnalysisResult());
                        unitContainer.setArgs(args);
                        unitContainer.setMethod(caller);
                        unitContainer.getDefinedFields().add(field);
                        unitContainer.setAnalysisResult(orthoAnalysis.getAnalysisResult());
                    }
                }

            }
        }

        return unitContainer;
    }

    public static List<String> getClassNamesFromDir(String clazzPath) {
        List<String> classNames = new ArrayList<>();

        File dir = new File(clazzPath);

        File[] files = dir.listFiles();
        if (files != null) {
            for (File clazz : files) {

                if (clazz.isDirectory()) {

                    List<String> newPaths = getClassNamesFromDir(clazz.getAbsolutePath());

                    for (String path : newPaths) {
                        classNames.add(clazz.getName() + "." + path);
                    }

                } else if (clazz.getName().endsWith(".class")) {

                    String className = clazz.getName();
                    classNames.add(className.substring(0, className.length() - ".class".length()));
                }
            }
        }

        return classNames;
    }

    protected static String javaHome;

    public static void setJavaHome(String javaHome) {

        if (javaHome == null || javaHome.isEmpty()) {
            // May also check necessary jars are available.
            throw new RuntimeException("Please set JAVA_HOME");
        }

        Utils.javaHome = javaHome;
    }

    public static String getJavaHome() {
        return javaHome;
    }
}
