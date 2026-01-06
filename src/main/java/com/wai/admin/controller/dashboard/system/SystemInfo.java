package com.wai.admin.controller.dashboard.system;

public class SystemInfo {
    private long uptime;
    private double[] loadAverage;
    private String osInfo;

    public SystemInfo() {}

    public SystemInfo(long uptime, double[] loadAverage, String osInfo) {
        this.uptime = uptime;
        this.loadAverage = loadAverage;
        this.osInfo = osInfo;
    }

    public static SystemInfoBuilder builder() {
        return new SystemInfoBuilder();
    }

    public static class SystemInfoBuilder {
        private long uptime;
        private double[] loadAverage;
        private String osInfo;

        public SystemInfoBuilder uptime(long uptime) {
            this.uptime = uptime;
            return this;
        }

        public SystemInfoBuilder loadAverage(double[] loadAverage) {
            this.loadAverage = loadAverage;
            return this;
        }

        public SystemInfoBuilder osInfo(String osInfo) {
            this.osInfo = osInfo;
            return this;
        }

        public SystemInfo build() {
            return new SystemInfo(uptime, loadAverage, osInfo);
        }
    }

    // Getters and Setters
    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    public double[] getLoadAverage() {
        return loadAverage;
    }

    public void setLoadAverage(double[] loadAverage) {
        this.loadAverage = loadAverage;
    }

    public String getOsInfo() {
        return osInfo;
    }

    public void setOsInfo(String osInfo) {
        this.osInfo = osInfo;
    }
}
