package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Map;

@Schema(description = "GitHub-style study heatmap data")
public record StudyHeatmapDto(
    @Schema(description = "Date to minutes mapping (yyyy-MM-dd -> minutes)")
    Map<String, Integer> heatmap
) implements Serializable {}
