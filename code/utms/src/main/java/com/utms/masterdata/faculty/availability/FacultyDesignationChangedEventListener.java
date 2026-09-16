package com.utms.masterdata.faculty.availability;

import com.utms.masterdata.faculty.FacultyDesignationChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;

/**
 * Listens for faculty designation changes and flags potential model mismatches.
 * When a faculty member's designation crosses the BLOCKED/AVAILABLE boundary,
 * their existing availability windows may no longer be semantically correct.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FacultyDesignationChangedEventListener {

    private static final Set<String> AVAILABLE_MODE_DESIGNATIONS = Set.of(
            "Visiting Faculty"
    );

    private final FacultyAvailabilityWindowRepository windowRepository;

    @TransactionalEventListener
    public void onDesignationChanged(FacultyDesignationChangedEvent event) {
        boolean wasDeclareAvailable = AVAILABLE_MODE_DESIGNATIONS.contains(event.previousDesignation());
        boolean nowDeclareAvailable = AVAILABLE_MODE_DESIGNATIONS.contains(event.newDesignation());

        if (wasDeclareAvailable != nowDeclareAvailable) {
            List<FacultyAvailabilityWindow> windows =
                    windowRepository.findByFacultyIdAndDeletedAtIsNull(event.facultyId());

            if (!windows.isEmpty()) {
                String previousMode = wasDeclareAvailable ? "AVAILABLE" : "BLOCKED";
                String newMode = nowDeclareAvailable ? "AVAILABLE" : "BLOCKED";

                log.warn("Faculty designation change causes model mismatch: facultyId={}, "
                                + "previousDesignation='{}', newDesignation='{}', "
                                + "modelChange={}->{}, existingWindows={}. "
                                + "Windows may need review.",
                        event.facultyId(),
                        event.previousDesignation(),
                        event.newDesignation(),
                        previousMode, newMode,
                        windows.size());
            }
        }
    }
}
