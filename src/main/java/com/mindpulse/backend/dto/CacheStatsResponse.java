package com.mindpulse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "缓存统计信息")
public class CacheStatsResponse {

    @Schema(description = "请求总数")
    private long totalRequests;

    @Schema(description = "缓存命中数")
    private long cacheHits;

    @Schema(description = "缓存未命中数")
    private long cacheMisses;

    @Schema(description = "缓存命中率")
    private double hitRate;

    @Schema(description = "平均响应时间（毫秒）")
    private long avgResponseTimeMs;

    @Schema(description = "缓存命中平均响应时间（毫秒）")
    private long cacheHitAvgMs;

    @Schema(description = "缓存未命中平均响应时间（毫秒）")
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
