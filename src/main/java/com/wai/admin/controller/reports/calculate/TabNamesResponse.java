package com.wai.admin.controller.reports.calculate;

public class TabNamesResponse {
    private boolean success;
    private int status;
    private String message;
    private TabNamesResponseData response;

    public TabNamesResponse() {}

    public TabNamesResponse(boolean success, int status, String message, TabNamesResponseData response) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.response = response;
    }

    public static TabNamesResponseBuilder builder() {
        return new TabNamesResponseBuilder();
    }

    public static class TabNamesResponseBuilder {
        private boolean success;
        private int status;
        private String message;
        private TabNamesResponseData response;

        public TabNamesResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public TabNamesResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        public TabNamesResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public TabNamesResponseBuilder response(TabNamesResponseData response) {
            this.response = response;
            return this;
        }

        public TabNamesResponse build() {
            return new TabNamesResponse(success, status, message, response);
        }
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TabNamesResponseData getResponse() {
        return response;
    }

    public void setResponse(TabNamesResponseData response) {
        this.response = response;
    }
}
