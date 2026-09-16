package com.utms.scheduling.engine.solver;

import com.utms.scheduling.engine.model.*;
import lombok.Getter;

import java.util.*;

/**
 * Mutable state of the CSP solver (KD-48).
 * Manages domains, assignments, and BitSet occupancy maps for O(1) conflict detection.
 *
 * <p>Slot addressing: {@link DaySlotRoom#slotIndex()} is the position of the slot WITHIN its
 * day's slot list ({@link #getSlotsForDay(int)}), not a position in the flat
 * {@code SchedulingInput.slotGrid}. Each physical (day, slot definition) therefore maps to exactly
 * one occupancy bit, so faculty/room/batch double-booking checks cannot be bypassed by addressing
 * the same period through another day's grid entries.
 */
public class CSPState {

    @Getter
    private final List<SessionVariable> variables;
    private final SchedulingInput input;

    private final List<List<DaySlotRoom>> domains;
    private final DaySlotRoom[] assignments;

    // BitSet occupancy maps (KD-48)
    private final Map<Long, BitSet> facultyOccupancy;
    private final BitSet[] roomOccupancy;
    private final Map<Long, BitSet> batchOccupancy;

    // Workload tracking (KD-53: hours, not sessions)
    private final Map<Long, double[]> facultyDailyHours;
    private final Map<Long, Double> facultyWeeklyHours;

    // Slot grid grouped by working-day index, each day's slots in grid (start-time) order
    private final List<List<SlotInfo>> slotsByDay;
    private final int maxSlotsPerDay;
    private final int numDays;
    @Getter
    private int assignedCount;

    public CSPState(List<SessionVariable> variables, SchedulingInput input) {
        this(variables, input, groupSlotsByDay(input));
    }

    private CSPState(List<SessionVariable> variables, SchedulingInput input, List<List<SlotInfo>> slotsByDay) {
        this.variables = variables;
        this.input = input;
        this.slotsByDay = slotsByDay;
        this.maxSlotsPerDay = slotsByDay.stream().mapToInt(List::size).max().orElse(0);
        this.numDays = input.getWorkingDays().size();
        this.assignments = new DaySlotRoom[variables.size()];
        this.domains = new ArrayList<>(variables.size());
        this.assignedCount = 0;
        this.facultyOccupancy = new HashMap<>();
        this.roomOccupancy = new BitSet[input.getRooms().size()];
        for (int i = 0; i < roomOccupancy.length; i++) {
            roomOccupancy[i] = new BitSet(numDays * maxSlotsPerDay);
        }
        this.batchOccupancy = new HashMap<>();
        this.facultyDailyHours = new HashMap<>();
        this.facultyWeeklyHours = new HashMap<>();
    }

    private static List<List<SlotInfo>> groupSlotsByDay(SchedulingInput input) {
        List<String> workingDays = input.getWorkingDays();
        List<List<SlotInfo>> byDay = new ArrayList<>(workingDays.size());
        for (String day : workingDays) {
            List<SlotInfo> daySlots = new ArrayList<>();
            for (SlotInfo si : input.getSlotGrid()) {
                if (day.equals(si.getDayOfWeek())) daySlots.add(si);
            }
            byDay.add(Collections.unmodifiableList(daySlots));
        }
        return Collections.unmodifiableList(byDay);
    }

    public void setDomains(List<List<DaySlotRoom>> domains) {
        this.domains.clear();
        this.domains.addAll(domains);
    }

    public List<DaySlotRoom> getDomain(int varIndex) { return domains.get(varIndex); }

    public boolean hasEmptyDomain() {
        for (int i = 0; i < variables.size(); i++) {
            if (assignments[i] == null && domains.get(i).isEmpty()) return true;
        }
        return false;
    }

    public void assign(int varIndex, DaySlotRoom value) {
        assignments[varIndex] = value;
        assignedCount++;
        SessionVariable var = variables.get(varIndex);
        int bitIndex = value.dayIndex() * maxSlotsPerDay + value.slotIndex();
        facultyOccupancy.computeIfAbsent(var.getFacultyId(), k -> new BitSet(numDays * maxSlotsPerDay)).set(bitIndex);
        roomOccupancy[value.roomIndex()].set(bitIndex);
        batchOccupancy.computeIfAbsent(var.getBatchId(), k -> new BitSet(numDays * maxSlotsPerDay)).set(bitIndex);
        double slotHours = getSlotDurationHours(value.dayIndex(), value.slotIndex());
        facultyDailyHours.computeIfAbsent(var.getFacultyId(), k -> new double[numDays]);
        facultyDailyHours.get(var.getFacultyId())[value.dayIndex()] += slotHours;
        facultyWeeklyHours.merge(var.getFacultyId(), slotHours, Double::sum);
    }

    public void unassign(int varIndex) {
        DaySlotRoom value = assignments[varIndex];
        if (value == null) return;
        SessionVariable var = variables.get(varIndex);
        int bitIndex = value.dayIndex() * maxSlotsPerDay + value.slotIndex();
        BitSet fOcc = facultyOccupancy.get(var.getFacultyId());
        if (fOcc != null) fOcc.clear(bitIndex);
        roomOccupancy[value.roomIndex()].clear(bitIndex);
        BitSet bOcc = batchOccupancy.get(var.getBatchId());
        if (bOcc != null) bOcc.clear(bitIndex);
        double slotHours = getSlotDurationHours(value.dayIndex(), value.slotIndex());
        double[] daily = facultyDailyHours.get(var.getFacultyId());
        if (daily != null) daily[value.dayIndex()] -= slotHours;
        facultyWeeklyHours.merge(var.getFacultyId(), -slotHours, Double::sum);
        assignments[varIndex] = null;
        assignedCount--;
    }

    public boolean isAssigned(int varIndex) { return assignments[varIndex] != null; }
    public DaySlotRoom getAssignment(int varIndex) { return assignments[varIndex]; }
    public boolean allAssigned() { return assignedCount == variables.size(); }
    public SessionVariable getVariable(int varIndex) { return variables.get(varIndex); }
    public int getVariableCount() { return variables.size(); }

    // O(1) occupancy queries (KD-48)
    public boolean getFacultyOccupancy(Long facultyId, int day, int slot) {
        BitSet bs = facultyOccupancy.get(facultyId);
        return bs != null && bs.get(day * maxSlotsPerDay + slot);
    }
    public boolean getRoomOccupancy(int roomIndex, int day, int slot) {
        return roomOccupancy[roomIndex].get(day * maxSlotsPerDay + slot);
    }
    public boolean getBatchOccupancy(Long batchId, int day, int slot) {
        BitSet bs = batchOccupancy.get(batchId);
        return bs != null && bs.get(day * maxSlotsPerDay + slot);
    }

    // Room queries
    public int getRoomCapacity(int roomIndex) { return input.getRooms().get(roomIndex).getCapacity(); }
    public boolean roomHasEquipment(int roomIndex, List<String> required) {
        if (required == null || required.isEmpty()) return true;
        List<String> tags = input.getRooms().get(roomIndex).getEquipmentTags();
        return tags != null && tags.containsAll(required);
    }
    public Long getActualRoomId(int roomIndex) { return input.getRooms().get(roomIndex).getRoomId(); }

    // Block queries
    public boolean isFacultyHardBlocked(Long facultyId, int day, int slot) {
        // TODO: Check against loaded faculty hard availability windows
        return false;
    }
    public boolean isRoomHardBlocked(int roomIndex, int day, int slot) {
        Long roomId = getActualRoomId(roomIndex);
        String dayName = input.getWorkingDays().get(day);
        for (ActiveBlock block : input.getActiveBlocks()) {
            if ("HARD".equals(block.getBlockType()) && block.getResourceId().equals(roomId)
                && block.getDayOfWeek().equals(dayName) && block.coversSlot(day, slot)) return true;
        }
        return false;
    }

    // Workload queries (KD-53, Fix #2)
    public boolean wouldExceedDailyLoad(Long facultyId, int day) {
        FacultyWorkloadLimits limits = getFacultyLimits(facultyId);
        if (limits == null) return false;
        double[] daily = facultyDailyHours.get(facultyId);
        return (daily != null ? daily[day] : 0.0) >= limits.getMaxDailyHours();
    }
    public boolean wouldExceedWeeklyLoad(Long facultyId) {
        FacultyWorkloadLimits limits = getFacultyLimits(facultyId);
        if (limits == null) return false;
        return facultyWeeklyHours.getOrDefault(facultyId, 0.0) >= limits.getMaxWeeklyHours();
    }
    public boolean wouldExceedConsecutive(Long facultyId, int day, int slot) {
        FacultyWorkloadLimits limits = getFacultyLimits(facultyId);
        if (limits == null) return false;
        double consecutiveHours = getSlotDurationHours(day, slot);
        for (int s = slot - 1; s >= 0; s--) {
            if (getFacultyOccupancy(facultyId, day, s)) consecutiveHours += getSlotDurationHours(day, s);
            else break;
        }
        int daySlotCount = slotsByDay.get(day).size();
        for (int s = slot + 1; s < daySlotCount; s++) {
            if (getFacultyOccupancy(facultyId, day, s)) consecutiveHours += getSlotDurationHours(day, s);
            else break;
        }
        return consecutiveHours > limits.getMaxConsecutiveHours();
    }

    public FacultyWorkloadLimits getFacultyLimits(Long facultyId) {
        return input.getFacultyLimits().stream().filter(l -> l.getFacultyId().equals(facultyId)).findFirst().orElse(null);
    }

    // Slot helpers (slotIndex is the position within the day's slot list)
    public List<SlotInfo> getSlotsForDay(int dayIndex) { return slotsByDay.get(dayIndex); }
    public double getSlotDurationHours(int dayIndex, int slotIndex) {
        List<SlotInfo> daySlots = slotsByDay.get(dayIndex);
        if (slotIndex < daySlots.size()) return daySlots.get(slotIndex).getDurationMinutes() / 60.0;
        return 1.0;
    }
    public Long getActualSlotId(int dayIndex, int slotIndex) {
        return slotsByDay.get(dayIndex).get(slotIndex).getSlotDefinitionId();
    }
    public String getDayName(int dayIndex) { return input.getWorkingDays().get(dayIndex); }

    // MRV variable selection
    public int selectMRVVariable() {
        int minSize = Integer.MAX_VALUE, selected = -1;
        for (int i = 0; i < variables.size(); i++) {
            if (assignments[i] != null) continue;
            int size = domains.get(i).size();
            if (size < minSize) { minSize = size; selected = i; }
        }
        return selected;
    }

    // LCV ordering (simplified — returns domain as-is)
    public List<DaySlotRoom> orderByLCV(int varIndex) { return new ArrayList<>(domains.get(varIndex)); }

    // Common slot pre-placement (KD-52)
    public void prePlaceCommonSlots(List<CommonSlotInfo> commonSlots) {
        for (CommonSlotInfo cs : commonSlots) {
            int dayIndex = input.getWorkingDays().indexOf(cs.getDayOfWeek());
            if (dayIndex < 0) continue;
            int slotIdx = resolveSlotIndex(dayIndex, cs.getSlotDefinitionId());
            if (slotIdx < 0) continue;
            int bitIndex = dayIndex * maxSlotsPerDay + slotIdx;
            for (int r = 0; r < roomOccupancy.length; r++) roomOccupancy[r].set(bitIndex);
            if (cs.isAppliesToAllBatches()) {
                for (SessionVariable var : variables)
                    batchOccupancy.computeIfAbsent(var.getBatchId(), k -> new BitSet(numDays * maxSlotsPerDay)).set(bitIndex);
            }
        }
    }

    /**
     * Fixed-session pre-placement (A4-14, KD-59). Generalizes common-slot pre-placement:
     * each fixed session (locked / approved / out-of-scope) occupies its SPECIFIC faculty,
     * room, and batch for its (day, slot), so free variables schedule around it without
     * hard-constraint violations (HC-LOCK-4). Unlike a common slot, a fixed session occupies
     * one specific room (not all rooms) and one specific faculty/batch.
     */
    public void prePlaceFixedSessions(List<FixedSessionInfo> fixedSessions) {
        for (FixedSessionInfo fs : fixedSessions) {
            int dayIndex = input.getWorkingDays().indexOf(fs.getDayOfWeek());
            if (dayIndex < 0) continue;
            int slotIdx = resolveSlotIndex(dayIndex, fs.getSlotDefinitionId());
            if (slotIdx < 0) continue;
            int bitIndex = dayIndex * maxSlotsPerDay + slotIdx;

            int roomIdx = resolveRoomIndex(fs.getRoomId());
            if (roomIdx >= 0) roomOccupancy[roomIdx].set(bitIndex);

            if (fs.getFacultyId() != null) {
                facultyOccupancy.computeIfAbsent(fs.getFacultyId(), k -> new BitSet(numDays * maxSlotsPerDay)).set(bitIndex);
            }
            if (fs.getBatchId() != null) {
                batchOccupancy.computeIfAbsent(fs.getBatchId(), k -> new BitSet(numDays * maxSlotsPerDay)).set(bitIndex);
            }
        }
    }

    private int resolveSlotIndex(int dayIndex, Long slotDefinitionId) {
        if (slotDefinitionId == null) return -1;
        List<SlotInfo> daySlots = slotsByDay.get(dayIndex);
        for (int i = 0; i < daySlots.size(); i++) {
            if (slotDefinitionId.equals(daySlots.get(i).getSlotDefinitionId())) return i;
        }
        return -1;
    }

    private int resolveRoomIndex(Long roomId) {
        if (roomId == null) return -1;
        for (int i = 0; i < input.getRooms().size(); i++) {
            if (roomId.equals(input.getRooms().get(i).getRoomId())) return i;
        }
        return -1;
    }

    // Checkpoint/Restore — stores full domain copies (Fix #2: size-based was incorrect)
    public Checkpoint checkpoint() {
        List<List<DaySlotRoom>> snapshots = new ArrayList<>(domains.size());
        for (List<DaySlotRoom> d : domains) snapshots.add(new ArrayList<>(d));
        return new Checkpoint(snapshots);
    }
    public void restore(Checkpoint cp) {
        for (int i = 0; i < domains.size(); i++) {
            domains.set(i, new ArrayList<>(cp.domainSnapshots().get(i)));
        }
    }
    public void forwardCheck(int assignedVar, DaySlotRoom value, HardConstraintValidator validator) {
        for (int i = 0; i < variables.size(); i++) {
            if (assignments[i] != null) continue;
            final int varIndex = i;
            domains.get(varIndex).removeIf(c -> !validator.isAssignmentValid(variables.get(varIndex), c, this));
        }
    }

    // Deep copy for optimization
    public CSPState deepCopy() {
        CSPState copy = new CSPState(this.variables, this.input, this.slotsByDay);
        List<List<DaySlotRoom>> dc = new ArrayList<>(domains.size());
        for (List<DaySlotRoom> d : domains) dc.add(new ArrayList<>(d));
        copy.setDomains(dc);
        for (int i = 0; i < assignments.length; i++) { if (assignments[i] != null) copy.assign(i, assignments[i]); }
        return copy;
    }

    public record Checkpoint(List<List<DaySlotRoom>> domainSnapshots) {}
}
