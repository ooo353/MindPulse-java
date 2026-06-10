package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "Category distribution data")
public record CategoryDistributionDto(
    @Schema(description = "Task category distribution")
    List<Map<String, Object>> taskCategories,

    @Schema(description = "Note category distribution")
    List<Map<String, Object>> noteCategories
) {}
