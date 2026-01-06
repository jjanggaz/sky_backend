package com.wai.admin.controller.dashboard.system;

public class Capacity {
    private long total;
    private long used;
    private long free;

    public Capacity() {}

    public Capacity(long total, long used, long free) {
        this.total = total;
        this.used = used;
        this.free = free;
    }

    public static CapacityBuilder builder() {
        return new CapacityBuilder();
    }

    public static class CapacityBuilder {
        private long total;
        private long used;
        private long free;

        public CapacityBuilder total(long total) {
            this.total = total;
            return this;
        }

        public CapacityBuilder used(long used) {
            this.used = used;
            return this;
        }

        public CapacityBuilder free(long free) {
            this.free = free;
            return this;
        }

        public Capacity build() {
            return new Capacity(total, used, free);
        }
    }

    // Getters and Setters
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getUsed() {
        return used;
    }

    public void setUsed(long used) {
        this.used = used;
    }

    public long getFree() {
        return free;
    }

    public void setFree(long free) {
        this.free = free;
    }
}
