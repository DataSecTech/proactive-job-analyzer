package com.datasectech.jobanalyzer.core.engine.model;

public class Issue {

    private int id;
    private String description;
    private String methodName;
    private int lineNumber;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString() {
        return "Issue{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", methodName='" + methodName + '\'' +
                ", lineNumber='" + lineNumber + '\'' +
                '}';
    }
}
