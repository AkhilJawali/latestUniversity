package com.utms.masterdata.academiccalendar;

/**
 * Working day pattern types per campus (FR-5).
 * FIVE_DAY: Mon-Fri working.
 * SIX_DAY: Mon-Sat working.
 * ALTERNATE_SATURDAY: specific Saturdays working (defined by working_saturdays field, e.g., "1,3" = 1st and 3rd).
 * CUSTOM: fully custom definition via custom_definition field.
 */
public enum PatternType {
    FIVE_DAY,
    SIX_DAY,
    ALTERNATE_SATURDAY,
    CUSTOM
}
