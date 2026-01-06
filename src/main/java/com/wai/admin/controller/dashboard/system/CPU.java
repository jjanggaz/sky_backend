package com.wai.admin.controller.dashboard.system;

public class CPU {
    private int usage;
    private int cores;
    private int temperature;

    public CPU() {}

    public CPU(int usage, int cores, int temperature) {
        this.usage = usage;
        this.cores = cores;
        this.temperature = temperature;
    }

    public static CPUBuilder builder() {
        return new CPUBuilder();
    }

    public static class CPUBuilder {
        private int usage;
        private int cores;
        private int temperature;

        public CPUBuilder usage(int usage) {
            this.usage = usage;
            return this;
        }

        public CPUBuilder cores(int cores) {
            this.cores = cores;
            return this;
        }

        public CPUBuilder temperature(int temperature) {
            this.temperature = temperature;
            return this;
        }

        public CPU build() {
            return new CPU(usage, cores, temperature);
        }
    }

    // Getters and Setters
    public int getUsage() {
        return usage;
    }

    public void setUsage(int usage) {
        this.usage = usage;
    }

    public int getCores() {
        return cores;
    }

    public void setCores(int cores) {
        this.cores = cores;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }
}
