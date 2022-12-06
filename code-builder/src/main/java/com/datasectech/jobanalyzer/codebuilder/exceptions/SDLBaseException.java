package com.datasectech.jobanalyzer.codebuilder.exceptions;

import com.datasectech.jobanalyzer.codebuilder.ProgramAnalysisOutput;

public class SDLBaseException extends RuntimeException {
    protected ProgramAnalysisOutput programAnalysisOutput;

    public ProgramAnalysisOutput getProgramAnalysisOutput() {
        return programAnalysisOutput;
    }

    public SDLBaseException(String message) {
        super(message);
    }
}
