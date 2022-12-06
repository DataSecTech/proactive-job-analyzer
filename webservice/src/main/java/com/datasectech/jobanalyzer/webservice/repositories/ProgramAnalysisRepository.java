package com.datasectech.jobanalyzer.webservice.repositories;

import com.datasectech.jobanalyzer.webservice.entities.ProgramAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(
        path = "program-analysis",
        itemResourceRel = "program-analysis",
        collectionResourceRel = "program-analysis-list"
)
public interface ProgramAnalysisRepository extends JpaRepository<ProgramAnalysisEntity, Long> {
    @Override
    @RestResource(exported = false)
    <S extends ProgramAnalysisEntity> S save(S entity);

    @Override
    @RestResource(exported = false)
    void delete(ProgramAnalysisEntity entity);
}
