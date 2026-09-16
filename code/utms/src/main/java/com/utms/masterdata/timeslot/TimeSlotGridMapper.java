package com.utms.masterdata.timeslot;

import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = BaseMapperConfig.class)
public interface TimeSlotGridMapper {

    @Mapping(target = "slots", source = "slotDefinitions")
    @Mapping(target = "campusId", source = "campusId")
    TimeSlotGridDto toDto(TimeSlotGrid grid);

    @Mapping(target = "durationMinutes", expression = "java(slot.getDurationMinutes())")
    @Mapping(target = "gridId", source = "gridId")
    SlotDefinitionDto toSlotDto(SlotDefinition slot);

    List<SlotDefinitionDto> toSlotDtoList(List<SlotDefinition> slots);

    @Mapping(target = "grid", ignore = true)
    @Mapping(target = "gridId", ignore = true)
    SlotDefinition toSlotEntity(CreateSlotDefinitionRequest request);
}
