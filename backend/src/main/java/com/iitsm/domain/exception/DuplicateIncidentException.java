package com.iitsm.domain.exception;

import java.util.Collections;
import java.util.List;

public class DuplicateIncidentException extends IitsmException {

    private final List<String> candidateDuplicateIds;
    private final List<Double> similarityScores;

    public DuplicateIncidentException(String message,
                                       List<String> candidateDuplicateIds,
                                       List<Double> similarityScores) {
        super(message, "INC_003", 409);
        this.candidateDuplicateIds = candidateDuplicateIds == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(candidateDuplicateIds);
        this.similarityScores = similarityScores == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(similarityScores);
    }

    public List<String> getCandidateDuplicateIds() {
        return candidateDuplicateIds;
    }

    public List<Double> getSimilarityScores() {
        return similarityScores;
    }
}
