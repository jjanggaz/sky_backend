package com.wai.admin.controller.dashboard.system;

public class ProjectStats {
    private long total;
    private long inProgress;
    private long completed;

    public ProjectStats() {}

    public ProjectStats(long total, long inProgress, long completed) {
        this.total = total;
        this.inProgress = inProgress;
        this.completed = completed;
    }

    public static ProjectStatsBuilder builder() {
        return new ProjectStatsBuilder();
    }

    public static class ProjectStatsBuilder {
        private long total;
        private long inProgress;
        private long completed;

        public ProjectStatsBuilder total(long total) {
            this.total = total;
            return this;
        }

        public ProjectStatsBuilder inProgress(long inProgress) {
            this.inProgress = inProgress;
            return this;
        }

        public ProjectStatsBuilder completed(long completed) {
            this.completed = completed;
            return this;
        }

        public ProjectStats build() {
            return new ProjectStats(total, inProgress, completed);
        }
    }

    // Getters and Setters
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getInProgress() {
        return inProgress;
    }

    public void setInProgress(long inProgress) {
        this.inProgress = inProgress;
    }

    public long getCompleted() {
        return completed;
    }

    public void setCompleted(long completed) {
        this.completed = completed;
    }
}
