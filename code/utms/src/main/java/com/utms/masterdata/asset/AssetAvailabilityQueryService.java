package com.utms.masterdata.asset;

import java.time.LocalTime;

/**
 * Query service for determining asset availability status.
 * Checks whether an asset is available during a given day and time window
 * based on its calendar windows and active resource blocks (KD-29).
 */
public interface AssetAvailabilityQueryService {

    /**
     * Returns whether an asset is available during the specified time window.
     * An asset is available if:
     * 1. It has an availability window covering the requested day and time range
     * 2. It is not blocked by an active resource block during that time
     *
     * @param assetId   the asset to check
     * @param dayOfWeek the day of week (e.g., "MONDAY")
     * @param startTime start of the requested time window
     * @param endTime   end of the requested time window
     * @return true if the asset is available, false otherwise
     */
    boolean isAssetAvailable(Long assetId, String dayOfWeek, LocalTime startTime, LocalTime endTime);
}
