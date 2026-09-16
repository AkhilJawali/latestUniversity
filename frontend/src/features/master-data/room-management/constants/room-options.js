// A4-430 §5.2 / FR-6 — room type options. Fixed backend enum (A4-6 RoomType, bound
// directly as @RequestParam/@RequestBody), so this IS a locked list, not free text.
export const ROOM_TYPES = ['CLASSROOM', 'LAB', 'SEMINAR_HALL', 'AUDITORIUM'];
