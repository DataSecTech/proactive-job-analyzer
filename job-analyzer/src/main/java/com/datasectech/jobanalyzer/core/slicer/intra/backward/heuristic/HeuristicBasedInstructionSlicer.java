package com.datasectech.jobanalyzer.core.slicer.intra.backward.heuristic;


import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.UnitContainer;
import com.datasectech.jobanalyzer.core.slicer.intra.backward.common.ValueArraySparseSet;
import com.datasectech.jobanalyzer.core.slicer.intra.backward.property.PropertyAnalysisResult;
import soot.ArrayType;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeuristicBasedInstructionSlicer extends BackwardFlowAnalysis {

    private FlowSet emptySet;
    private String slicingCriteria;
    private String method;
    private Map<String, List<PropertyAnalysisResult>> propertyUseMap;

    public HeuristicBasedInstructionSlicer(DirectedGraph g, String slicingCriteria, String method) {
        super(g);
        this.emptySet = new ValueArraySparseSet();
        this.slicingCriteria = slicingCriteria;
        this.method = method;
        this.propertyUseMap = new HashMap<>();
        doAnalysis();
    }

    @Override
    protected void flowThrough(Object in, Object node, Object out) {
        FlowSet inSet = (FlowSet) in,
                outSet = (FlowSet) out;
        Unit currInstruction = (Unit) node;

        if (currInstruction.toString().startsWith(slicingCriteria)) {
            addCurrInstInOutSet(outSet, currInstruction);
            return;
        }

        if (!inSet.isEmpty()) {

            outSet.union(inSet);

            if (currInstruction.toString().startsWith("if ")) {
                return;
            }


            for (Object anInSet : inSet.toList()) {

                UnitContainer insetInstruction = (UnitContainer) anInSet;
                List<ValueBox> useBoxes = insetInstruction.getUnit().getUseBoxes();

                for (ValueBox usebox : useBoxes) {

                    if ((usebox.getValue().toString().equals("r0") && insetInstruction.getUnit().toString().contains("r0.")) ||
                            (usebox.getValue().toString().equals("this") && insetInstruction.getUnit().toString().contains("this."))) {
                        continue;
                    }

                    if (isArgOfAssignInvoke(usebox, insetInstruction.getUnit())) {
                        continue;
                    }

                    if (insetInstruction.getUnit().toString().contains("[" + usebox.getValue() + "]")) {
                        continue;
                    }

                    if (isSpecialInvokeOn(currInstruction, usebox)) {
                        addCurrInstInOutSet(outSet, currInstruction);
                        return;
                    }

                    for (ValueBox defbox : currInstruction.getDefBoxes()) {

                        if ((defbox.getValue().toString().equals("r0") && currInstruction.toString().startsWith("r0.")) ||
                                (defbox.getValue().toString().equals("this") && currInstruction.toString().startsWith("this."))) {
                            continue;
                        }

                        if (defbox.getValue().equivTo(usebox.getValue())) {

                            addCurrInstInOutSet(outSet, currInstruction);
                            return;

                        } else if (defbox.getValue().toString().contains(usebox.getValue().toString())) {
                            if (usebox.getValue().getType() instanceof ArrayType) {

                                addCurrInstInOutSet(outSet, currInstruction);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private static final List<String> ASSIGN_WHITE_LISTED_METHODS = new ArrayList<>();
    private static final List<String> INVOKE_WHITE_LISTED_METHODS = new ArrayList<>();

    static {
        ASSIGN_WHITE_LISTED_METHODS.add("<javax.xml.bind.DatatypeConverterInterface: byte[] parseBase64Binary(java.lang.String)>");
        ASSIGN_WHITE_LISTED_METHODS.add("<javax.xml.bind.DatatypeConverterInterface: byte[] parseHexBinary(java.lang.String)>");
        ASSIGN_WHITE_LISTED_METHODS.add("<java.util.Arrays: byte[] copyOf(byte[],int)>");

        INVOKE_WHITE_LISTED_METHODS.add("<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>");
        INVOKE_WHITE_LISTED_METHODS.add("<java.lang.String: void <init>");
    }

    private boolean isArgOfAssignInvoke(ValueBox useBox, Unit unit) {

        if (unit instanceof JAssignStmt && unit.toString().contains("invoke ")) {

            for (String whitelisted : ASSIGN_WHITE_LISTED_METHODS) {
                if (unit.toString().contains(whitelisted)) {
                    return false;
                }
            }

            InvokeExpr invokeExpr = ((JAssignStmt) unit).getInvokeExpr();
            List<Value> args = invokeExpr.getArgs();
            for (Value arg : args) {
                if (arg.equivTo(useBox.getValue())) {
                    return true;
                }
            }
        }

        if (unit.toString().contains(" newarray ")) {
            for (ValueBox valueBox : unit.getUseBoxes()) {
                if (valueBox.getValue().equivTo(useBox.getValue())) {
                    return true;
                }
            }
        }

        return false;
    }

    private void addCurrInstInOutSet(FlowSet outSet, Unit currInstruction) {

        UnitContainer currUnitContainer = new UnitContainer();
        currUnitContainer.setUnit(currInstruction);
        currUnitContainer.setMethod(method);

        outSet.add(currUnitContainer);
    }

    private boolean isSpecialInvokeOn(Unit currInstruction, ValueBox usebox) {
        boolean specialinvoke = currInstruction instanceof JInvokeStmt && currInstruction.toString().contains("specialinvoke")
                && currInstruction.toString().contains(usebox.getValue().toString() + ".<");

        for (String whitelisted : INVOKE_WHITE_LISTED_METHODS) {
            if (currInstruction.toString().contains(whitelisted) &&
                    currInstruction.toString().contains(", " + usebox.getValue().toString() + ",")) {
                specialinvoke = true;
                break;
            }
        }

        return specialinvoke;
    }

    @Override
    protected Object newInitialFlow() {
        return emptySet.clone();
    }

    @Override
    protected Object entryInitialFlow() {
        return emptySet.clone();
    }

    @Override
    protected void merge(Object in1, Object in2, Object out) {
        FlowSet inSet1 = (FlowSet) in1,
                inSet2 = (FlowSet) in2,
                outSet = (FlowSet) out;

        inSet1.union(inSet2, outSet);
    }

    @Override
    protected void copy(Object source, Object dest) {
        FlowSet srcSet = (FlowSet) source,
                destSet = (FlowSet) dest;
        srcSet.copy(destSet);
    }

    public Map<String, List<PropertyAnalysisResult>> getPropertyUseMap() {
        return propertyUseMap;
    }
}
