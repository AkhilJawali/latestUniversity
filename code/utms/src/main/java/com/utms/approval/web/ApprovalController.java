package com.utms.approval.web;

import com.utms.approval.dto.ApprovalPipelineDto;
import com.utms.approval.dto.ApproveRequest;
import com.utms.approval.dto.RejectRequest;
import com.utms.approval.dto.SubmitDraftRequest;
import com.utms.approval.dto.WorkflowInstanceDto;
import com.utms.approval.enums.WorkflowState;
import com.utms.approval.service.ApprovalWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A4-19 — REST endpoints for the multi-level approval workflow. Responses return DTOs
 * directly (consistent with the scheduling module). {@code actorUserId} is resolved from the
 * request context in the service — never from the body. RBAC is deferred (endpoints are
 * permitAll; see the // TODO markers) until the Auth module lands (PD-103).
 */
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Tag(name = "Approvals", description = "Multi-level timetable-draft approval workflow")
public class ApprovalController {

    private final ApprovalWorkflowService service;

    @PostMapping("/submit")
    @Operation(summary = "Submit a draft into the approval workflow")
    // TODO: @PreAuthorize("hasRole('COORDINATOR')") — enable when Auth module is built
    public ResponseEntity<WorkflowInstanceDto> submit(@Valid @RequestBody SubmitDraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(request));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve at the current level (advances or finalises)")
    // TODO: @PreAuthorize — enable when Auth module is built
    public ResponseEntity<WorkflowInstanceDto> approve(@PathVariable Long id,
                                                       @Valid @RequestBody ApproveRequest request) {
        return ResponseEntity.ok(service.approve(id, request));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject at the current level (returns to the previous level with a reason)")
    // TODO: @PreAuthorize — enable when Auth module is built
    public ResponseEntity<WorkflowInstanceDto> reject(@PathVariable Long id,
                                                      @Valid @RequestBody RejectRequest request) {
        return ResponseEntity.ok(service.reject(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a workflow instance with its full approval history")
    public ResponseEntity<WorkflowInstanceDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/draft/{draftId}")
    @Operation(summary = "Get the workflow instance for a draft")
    public ResponseEntity<WorkflowInstanceDto> getByDraft(@PathVariable Long draftId) {
        return ResponseEntity.ok(service.getByDraft(draftId));
    }

    @GetMapping
    @Operation(summary = "List workflow instances (reviewer queue), filterable by state and level")
    public ResponseEntity<Page<WorkflowInstanceDto>> list(
            @RequestParam(required = false) WorkflowState state,
            @RequestParam(required = false) Integer levelIndex,
            Pageable pageable) {
        return ResponseEntity.ok(service.list(state, levelIndex, pageable));
    }

    @GetMapping("/pipelines")
    @Operation(summary = "List configurable approval pipelines with their levels")
    public ResponseEntity<List<ApprovalPipelineDto>> listPipelines() {
        return ResponseEntity.ok(service.listPipelines());
    }
}
