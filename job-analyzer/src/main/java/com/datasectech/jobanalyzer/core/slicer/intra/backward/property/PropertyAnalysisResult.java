package com.datasectech.jobanalyzer.core.slicer.intra.backward.property;

import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.MethodWrapper;
import com.datasectech.jobanalyzer.core.slicer.inter.backward.common.UnitContainer;

import java.util.List;
import java.util.Map;

public class PropertyAnalysisResult {

    private MethodWrapper methodWrapper;
    private List<Integer> influencingParams;
    private List<UnitContainer> slicingResult;
    private Map<String, List<PropertyAnalysisResult>> propertyUseMap;

    public MethodWrapper getMethodWrapper() {
        return methodWrapper;
    }

    public void setMethodWrapper(MethodWrapper methodWrapper) {
        this.methodWrapper = methodWrapper;
    }

    public List<Integer> getInfluencingParams() {
        return influencingParams;
    }

    public void setInfluencingParams(List<Integer> influencingParams) {
        this.influencingParams = influencingParams;
    }

    public List<UnitContainer> getSlicingResult() {
        return slicingResult;
    }

    public void setSlicingResult(List<UnitContainer> slicingResult) {
        this.slicingResult = slicingResult;
    }

    public Map<String, List<PropertyAnalysisResult>> getPropertyUseMap() {
        return propertyUseMap;
    }

    public void setPropertyUseMap(Map<String, List<PropertyAnalysisResult>> propertyUseMap) {
        this.propertyUseMap = propertyUseMap;
    }
}
