package com.utms.approval.mapper;

import com.utms.approval.dto.ApprovalLevelDto;
import com.utms.approval.dto.ApprovalPipelineDto;
import com.utms.approval.dto.WorkflowStepDto;
import com.utms.approval.entity.ApprovalLevel;
import com.utms.approval.entity.ApprovalPipeline;
import com.utms.approval.entity.WorkflowStep;
import com.utms.common.mapper.BaseMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mappers for the approval module (A4-19). The {@code WorkflowInstanceDto} itself
 * is assembled in the service (it needs the resolved level name, the draft status, and the
 * ordered step list), so only the leaf DTOs are mapped here.
 */
@Mapper(config = BaseMapperConfig.class)
public interface ApprovalMapper {

    WorkflowStepDto toStepDto(WorkflowStep step);

    List<WorkflowStepDto> toStepDtos(List<WorkflowStep> steps);

    ApprovalLevelDto toLevelDto(ApprovalLevel level);

    List<ApprovalLevelDto> toLevelDtos(List<ApprovalLevel> levels);

    @Mapping(target = "levels", ignore = true) // set in the service after mapping levels
    ApprovalPipelineDto toPipelineDto(ApprovalPipeline pipeline);
}
