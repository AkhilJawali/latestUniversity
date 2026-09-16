package com.utms.approval.service;

import com.utms.approval.entity.ApprovalLevel;
import com.utms.approval.entity.ApprovalPipeline;
import com.utms.approval.repository.ApprovalLevelRepository;
import com.utms.approval.repository.ApprovalPipelineRepository;
import com.utms.common.exception.BusinessRuleViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the applicable approval pipeline and its ordered levels (A4-19 FR-6.3 / PD-101,
 * PD-102). For this story: the single active global pipeline. Encapsulates the level
 * traversal semantics (KD-A19-2): level 0 is the drafting/Coordinator level; review starts
 * at level 1; the final level is the highest index.
 */
@Component
@RequiredArgsConstructor
public class PipelineResolver {

    private final ApprovalPipelineRepository pipelineRepository;
    private final ApprovalLevelRepository levelRepository;

    /** Immutable view of a pipeline's ordered levels with traversal helpers. */
    public static final class ResolvedPipeline {
        private final ApprovalPipeline pipeline;
        private final List<ApprovalLevel> levels; // ordered by levelIndex asc

        ResolvedPipeline(ApprovalPipeline pipeline, List<ApprovalLevel> levels) {
            this.pipeline = pipeline;
            this.levels = levels;
        }

        public Long pipelineId() {
            return pipeline.getId();
        }

        public ApprovalPipeline pipeline() {
            return pipeline;
        }

        public List<ApprovalLevel> levels() {
            return levels;
        }

        /** First review level index (KD-A19-2): level 1 when a drafting level 0 exists,
         *  else level 0 (single-level pipeline). */
        public int firstReviewLevelIndex() {
            return levels.size() > 1 ? 1 : 0;
        }

        public boolean hasNextLevel(int currentLevelIndex) {
            return currentLevelIndex < maxLevelIndex();
        }

        public boolean hasPreviousReviewLevel(int currentLevelIndex) {
            return currentLevelIndex > firstReviewLevelIndex();
        }

        public int maxLevelIndex() {
            return levels.get(levels.size() - 1).getLevelIndex();
        }

        public String levelName(int levelIndex) {
            return levels.stream()
                    .filter(l -> l.getLevelIndex() == levelIndex)
                    .map(ApprovalLevel::getLevelName)
                    .findFirst()
                    .orElse("Level " + levelIndex);
        }
    }

    /** The active pipeline used for new submissions (PD-102). */
    public ResolvedPipeline resolveActive() {
        ApprovalPipeline pipeline = pipelineRepository
                .findFirstByIsActiveTrueAndDeletedAtIsNullOrderByIdAsc()
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "No active approval pipeline is configured"));
        return load(pipeline);
    }

    /** The pipeline an existing instance was created under. */
    public ResolvedPipeline resolveById(Long pipelineId) {
        ApprovalPipeline pipeline = pipelineRepository
                .findByIdAndDeletedAtIsNull(pipelineId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Approval pipeline not found: " + pipelineId));
        return load(pipeline);
    }

    private ResolvedPipeline load(ApprovalPipeline pipeline) {
        List<ApprovalLevel> levels = levelRepository
                .findByPipelineIdAndDeletedAtIsNullOrderByLevelIndexAsc(pipeline.getId());
        if (levels.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Approval pipeline has no levels: " + pipeline.getId());
        }
        return new ResolvedPipeline(pipeline, levels);
    }
}
