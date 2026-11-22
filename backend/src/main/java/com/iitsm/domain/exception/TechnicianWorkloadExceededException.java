package com.iitsm.domain.exception;

public class TechnicianWorkloadExceededException extends IitsmException {

    private final Long technicianId;
    private final int currentLoad;
    private final int maxLoad;

    public TechnicianWorkloadExceededException(Long technicianId, int currentLoad, int maxLoad) {
        super("Technician " + technicianId + " workload exceeded: current=" + currentLoad
                + ", max=" + maxLoad, "TECH_002", 409);
        this.technicianId = technicianId;
        this.currentLoad = currentLoad;
        this.maxLoad = maxLoad;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public int getCurrentLoad() {
        return currentLoad;
    }

    public int getMaxLoad() {
        return maxLoad;
    }
}
