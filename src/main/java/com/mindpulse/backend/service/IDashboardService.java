package com.mindpulse.backend.service;

import com.mindpulse.backend.dto.CategoryDistributionDto;
import com.mindpulse.backend.dto.DashboardSummaryDto;
import com.mindpulse.backend.dto.ProductivityDto;
import com.mindpulse.backend.dto.StudyHeatmapDto;

public interface IDashboardService {
    DashboardSummaryDto getSummary(String userId);
    ProductivityDto getProductivity(String userId, String period);
    CategoryDistributionDto getCategoryDistribution(String userId);
    StudyHeatmapDto getStudyHeatmap(String userId, int year);
}
