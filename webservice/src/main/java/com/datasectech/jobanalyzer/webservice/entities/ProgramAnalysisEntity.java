package com.datasectech.jobanalyzer.webservice.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "program_analysis")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProgramAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "input_snippet", columnDefinition = "TEXT")
    private String inputSnippet;

    @Column(name = "generated_class", columnDefinition = "TEXT")
    private String generatedClass;

    @Nullable
    @Column(name = "compiled_output", columnDefinition = "BLOB")
    private byte[] compiledOutput;

    @Nullable
    @Column(name = "analysis_output", columnDefinition = "BLOB")
    private byte[] analysisOutput;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInputSnippet() {
        return inputSnippet;
    }

    public void setInputSnippet(String inputSnippet) {
        this.inputSnippet = inputSnippet;
    }

    public String getGeneratedClass() {
        return generatedClass;
    }

    public void setGeneratedClass(String generatedClass) {
        this.generatedClass = generatedClass;
    }

    @Nullable
    public byte[] getCompiledOutput() {
        return compiledOutput;
    }

    public void setCompiledOutput(@Nullable byte[] compiledOutput) {
        this.compiledOutput = compiledOutput;
    }

    @Nullable
    public byte[] getAnalysisOutput() {
        return analysisOutput;
    }

    public void setAnalysisOutput(@Nullable byte[] analysisOutput) {
        this.analysisOutput = analysisOutput;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
}
