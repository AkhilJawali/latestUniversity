package com.utms.masterdata.block;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * KD-37: Boundary-aware reporting for resource blocks.
 * Calculates block duration considering only the portion within the requested date range.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockReportService {

    private final ResourceBlockRepository blockRepository;

    /**
     * Generate a report of active blocks for a resource within a date range.
     * Boundary-aware: only counts days within the requested window, not the full block span.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> generateReport(String resourceType, Long resourceId,
                                               LocalDate startDate, LocalDate endDate) {
        List<ResourceBlock> activeBlocks = blockRepository
                .findByResourceTypeAndResourceIdAndStatusAndDeletedAtIsNull(
                        resourceType, resourceId, "ACTIVE");

        // Filter blocks that overlap with the requested date range
        List<Map<String, Object>> blockSummaries = activeBlocks.stream()
                .filter(block -> !block.getEndDate().isBefore(startDate) && !block.getStartDate().isAfter(endDate))
                .map(block -> {
                    // KD-37: Boundary-aware calculation — clip to requested window
                    LocalDate effectiveStart = block.getStartDate().isBefore(startDate)
                            ? startDate : block.getStartDate();
                    LocalDate effectiveEnd = block.getEndDate().isAfter(endDate)
                            ? endDate : block.getEndDate();
                    long daysBlocked = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;

                    return Map.<String, Object>of(
                            "blockId", block.getId(),
                            "blockType", block.getBlockType(),
                            "reasonCode", block.getReasonCode(),
                            "effectiveStart", effectiveStart.toString(),
                            "effectiveEnd", effectiveEnd.toString(),
                            "daysBlocked", daysBlocked,
                            "startTime", block.getStartTime().toString(),
                            "endTime", block.getEndTime().toString()
                    );
                })
                .toList();

        long totalDaysInRange = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long totalBlockedDays = blockSummaries.stream()
                .mapToLong(s -> (Long) s.get("daysBlocked"))
                .sum();

        return Map.of(
                "resourceType", resourceType,
                "resourceId", resourceId,
                "reportStartDate", startDate.toString(),
                "reportEndDate", endDate.toString(),
                "totalDaysInRange", totalDaysInRange,
                "totalBlockedDays", totalBlockedDays,
                "blocks", blockSummaries
        );
    }
}
