package com.datasectech.jobanalyzer.codebuilder.exceptions;


import com.datasectech.jobanalyzer.codebuilder.ProgramAnalysisOutput;

public class ProgramAnalysisException extends SDLBaseException {

    public ProgramAnalysisException(ProgramAnalysisOutput programAnalysisOutput, String message) {
        super(message);
        this.programAnalysisOutput = programAnalysisOutput;
    }

    public ProgramAnalysisException(String message) {
        super(message);
    }
}
