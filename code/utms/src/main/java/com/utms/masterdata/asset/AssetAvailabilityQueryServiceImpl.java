package com.utms.masterdata.asset;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetAvailabilityQueryServiceImpl implements AssetAvailabilityQueryService {

    private final AssetAvailabilityWindowRepository windowRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isAssetAvailable(Long assetId, String dayOfWeek, LocalTime startTime, LocalTime endTime) {
        // Check if the asset has a calendar window covering the requested time
        List<AssetAvailabilityWindow> windows = windowRepository
                .findByAssetIdAndDayOfWeekAndDeletedAtIsNull(assetId, dayOfWeek);

        boolean withinCalendar = windows.stream()
                .anyMatch(w -> !w.getStartTime().isAfter(startTime) && !w.getEndTime().isBefore(endTime));

        if (!withinCalendar) {
            return false;
        }

        // TODO: Check active resource blocks on this asset for the given time window
        //       once A4-8 (resource block module) is built (KD-29)

        return true;
    }
}
