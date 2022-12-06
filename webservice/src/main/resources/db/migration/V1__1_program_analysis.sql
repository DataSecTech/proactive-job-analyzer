CREATE TABLE program_analysis
(
    `id`              bigint(20) NOT NULL AUTO_INCREMENT,
    `input_snippet`   TEXT       NOT NULL,
    `generated_class` TEXT       NOT NULL,
    `compiled_output` BLOB,
    `analysis_output` BLOB,
    `started_at`      datetime   NOT NULL,
    `ended_at`        datetime   NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
