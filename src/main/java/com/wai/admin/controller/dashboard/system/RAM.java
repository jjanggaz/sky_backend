package com.wai.admin.controller.dashboard.system;

public class RAM {
    private long total;
    private long used;
    private long free;
    private int usage;

    public RAM() {}

    public RAM(long total, long used, long free, int usage) {
        this.total = total;
        this.used = used;
        this.free = free;
        this.usage = usage;
    }

    public static RAMBuilder builder() {
        return new RAMBuilder();
    }

    public static class RAMBuilder {
        private long total;
        private long used;
        private long free;
        private int usage;

        public RAMBuilder total(long total) {
            this.total = total;
            return this;
        }

        public RAMBuilder used(long used) {
            this.used = used;
            return this;
        }

        public RAMBuilder free(long free) {
            this.free = free;
            return this;
        }

        public RAMBuilder usage(int usage) {
            this.usage = usage;
            return this;
        }

        public RAM build() {
            return new RAM(total, used, free, usage);
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

    public int getUsage() {
        return usage;
    }

    public void setUsage(int usage) {
        this.usage = usage;
    }
}
