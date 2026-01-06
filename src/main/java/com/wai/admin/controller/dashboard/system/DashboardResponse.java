package com.wai.admin.controller.dashboard.system;

public class DashboardResponse {
    private boolean success;
    private String message;
    private DashboardData data;
    private String error;

    public DashboardResponse() {}

    public DashboardResponse(boolean success, String message, DashboardData data, String error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
    }

    public static DashboardResponseBuilder builder() {
        return new DashboardResponseBuilder();
    }

    public static class DashboardResponseBuilder {
        private boolean success;
        private String message;
        private DashboardData data;
        private String error;

        public DashboardResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public DashboardResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public DashboardResponseBuilder data(DashboardData data) {
            this.data = data;
            return this;
        }

        public DashboardResponseBuilder error(String error) {
            this.error = error;
            return this;
        }

        public DashboardResponse build() {
            return new DashboardResponse(success, message, data, error);
        }
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DashboardData getData() {
        return data;
    }

    public void setData(DashboardData data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
