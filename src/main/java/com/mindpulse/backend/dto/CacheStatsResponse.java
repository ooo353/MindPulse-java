package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cache statistics information")
public class CacheStatsResponse {

    @Schema(description = "Total request count")
    private long totalRequests;

    @Schema(description = "Cache hit count")
    private long cacheHits;

    @Schema(description = "Cache miss count")
    private long cacheMisses;

    @Schema(description = "Cache hit rate")
    private double hitRate;

    @Schema(description = "Average response time in milliseconds")
    private long avgResponseTimeMs;

    @Schema(description = "Cache hit average response time in milliseconds")
    private long cacheHitAvgMs;

    @Schema(description = "Cache miss average response time in milliseconds")
    private long cacheMissAvgMs;

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getCacheHits() {
        return cacheHits;
    }

    public void setCacheHits(long cacheHits) {
        this.cacheHits = cacheHits;
    }

    public long getCacheMisses() {
        return cacheMisses;
    }

    public void setCacheMisses(long cacheMisses) {
        this.cacheMisses = cacheMisses;
    }

    public double getHitRate() {
        return hitRate;
    }

    public void setHitRate(double hitRate) {
        this.hitRate = hitRate;
    }

    public long getAvgResponseTimeMs() {
        return avgResponseTimeMs;
    }

    public void setAvgResponseTimeMs(long avgResponseTimeMs) {
        this.avgResponseTimeMs = avgResponseTimeMs;
    }

    public long getCacheHitAvgMs() {
        return cacheHitAvgMs;
    }

    public void setCacheHitAvgMs(long cacheHitAvgMs) {
        this.cacheHitAvgMs = cacheHitAvgMs;
    }

    public long getCacheMissAvgMs() {
        return cacheMissAvgMs;
    }

    public void setCacheMissAvgMs(long cacheMissAvgMs) {
        this.cacheMissAvgMs = cacheMissAvgMs;
    }
}
