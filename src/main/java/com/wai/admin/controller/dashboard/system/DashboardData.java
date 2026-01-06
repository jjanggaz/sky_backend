package com.wai.admin.controller.dashboard.system;

import com.wai.admin.controller.dashboard.system.ServerStatus;
import com.wai.admin.controller.dashboard.system.ProjectStats;
import com.wai.admin.controller.dashboard.system.ModelStats;
import com.wai.admin.controller.dashboard.system.ApprovalStats;

public class DashboardData {
    private ServerStatus serverStatus;
    private ProjectStats projectStats;
    private ModelStats modelStats;
    private ApprovalStats approvalStats;

    public DashboardData() {}

    public DashboardData(ServerStatus serverStatus, ProjectStats projectStats, ModelStats modelStats, ApprovalStats approvalStats) {
        this.serverStatus = serverStatus;
        this.projectStats = projectStats;
        this.modelStats = modelStats;
        this.approvalStats = approvalStats;
    }

    public static DashboardDataBuilder builder() {
        return new DashboardDataBuilder();
    }

    public static class DashboardDataBuilder {
        private ServerStatus serverStatus;
        private ProjectStats projectStats;
        private ModelStats modelStats;
        private ApprovalStats approvalStats;

        public DashboardDataBuilder serverStatus(ServerStatus serverStatus) {
            this.serverStatus = serverStatus;
            return this;
        }

        public DashboardDataBuilder projectStats(ProjectStats projectStats) {
            this.projectStats = projectStats;
            return this;
        }

        public DashboardDataBuilder modelStats(ModelStats modelStats) {
            this.modelStats = modelStats;
            return this;
        }

        public DashboardDataBuilder approvalStats(ApprovalStats approvalStats) {
            this.approvalStats = approvalStats;
            return this;
        }

        public DashboardData build() {
            return new DashboardData(serverStatus, projectStats, modelStats, approvalStats);
        }
    }

    // Getters and Setters
    public ServerStatus getServerStatus() {
        return serverStatus;
    }

    public void setServerStatus(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    public ProjectStats getProjectStats() {
        return projectStats;
    }

    public void setProjectStats(ProjectStats projectStats) {
        this.projectStats = projectStats;
    }

    public ModelStats getModelStats() {
        return modelStats;
    }

    public void setModelStats(ModelStats modelStats) {
        this.modelStats = modelStats;
    }

    public ApprovalStats getApprovalStats() {
        return approvalStats;
    }

    public void setApprovalStats(ApprovalStats approvalStats) {
        this.approvalStats = approvalStats;
    }
}
