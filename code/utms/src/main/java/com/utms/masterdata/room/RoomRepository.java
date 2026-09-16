package com.utms.masterdata.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    boolean existsByCodeAndCampusIdAndDeletedAtIsNull(String code, Long campusId);

    Optional<Room> findByIdAndDeletedAtIsNull(Long id);

    /** A4-fix (scheduling data loader) — all active rooms on a campus. */
    List<Room> findByCampusIdAndDeletedAtIsNull(Long campusId);
}
