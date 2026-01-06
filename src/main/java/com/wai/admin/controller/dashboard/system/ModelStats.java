package com.wai.admin.controller.dashboard.system;

public class ModelStats {
    private long total;
    private long pending;
    private long approved;

    public ModelStats() {}

    public ModelStats(long total, long pending, long approved) {
        this.total = total;
        this.pending = pending;
        this.approved = approved;
    }

    public static ModelStatsBuilder builder() {
        return new ModelStatsBuilder();
    }

    public static class ModelStatsBuilder {
        private long total;
        private long pending;
        private long approved;

        public ModelStatsBuilder total(long total) {
            this.total = total;
            return this;
        }

        public ModelStatsBuilder pending(long pending) {
            this.pending = pending;
            return this;
        }

        public ModelStatsBuilder approved(long approved) {
            this.approved = approved;
            return this;
        }

        public ModelStats build() {
            return new ModelStats(total, pending, approved);
        }
    }

    // Getters and Setters
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public long getApproved() {
        return approved;
    }

    public void setApproved(long approved) {
        this.approved = approved;
    }
}
