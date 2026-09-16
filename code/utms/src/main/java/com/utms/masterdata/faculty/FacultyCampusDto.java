package com.utms.masterdata.faculty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Response DTO for a faculty member's campus association.
 *
 * <p>Added as an A4-4 follow-up (OQ-1 from the A4-420 frontend story): the module
 * previously had no way to read back a faculty's current campus associations, so the
 * faculty-management UI could add/remove campuses but not display the current set.
 * This exposes campus id + name + code per active association.</p>
 */
@Getter
@Setter
@Builder
public class FacultyCampusDto {

    private Long campusId;
    private String name;
    private String code;
}
