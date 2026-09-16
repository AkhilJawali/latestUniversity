package com.utms.masterdata.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetAvailabilityWindowRepository extends JpaRepository<AssetAvailabilityWindow, Long> {

    List<AssetAvailabilityWindow> findByAssetIdAndDeletedAtIsNull(Long assetId);

    List<AssetAvailabilityWindow> findByAssetIdAndDayOfWeekAndDeletedAtIsNull(Long assetId, String dayOfWeek);
}
