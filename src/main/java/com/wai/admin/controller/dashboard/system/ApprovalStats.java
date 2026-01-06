package com.wai.admin.controller.dashboard.system;

public class ApprovalStats {
    private long total;
    private long pending;
    private long approved;
    private long rejected;

    public ApprovalStats() {}

    public ApprovalStats(long total, long pending, long approved, long rejected) {
        this.total = total;
        this.pending = pending;
        this.approved = approved;
        this.rejected = rejected;
    }

    public static ApprovalStatsBuilder builder() {
        return new ApprovalStatsBuilder();
    }

    public static class ApprovalStatsBuilder {
        private long total;
        private long pending;
        private long approved;
        private long rejected;

        public ApprovalStatsBuilder total(long total) {
            this.total = total;
            return this;
        }

        public ApprovalStatsBuilder pending(long pending) {
            this.pending = pending;
            return this;
        }

        public ApprovalStatsBuilder approved(long approved) {
            this.approved = approved;
            return this;
        }

        public ApprovalStatsBuilder rejected(long rejected) {
            this.rejected = rejected;
            return this;
        }

        public ApprovalStats build() {
            return new ApprovalStats(total, pending, approved, rejected);
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

    public long getRejected() {
        return rejected;
    }

    public void setRejected(long rejected) {
        this.rejected = rejected;
    }
}
