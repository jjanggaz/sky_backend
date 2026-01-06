package com.wai.admin.controller.dashboard.system;

public class ServerStatus {
    private Capacity capacity;
    private CPU cpu;
    private RAM ram;
    private SystemInfo system;

    public ServerStatus() {}

    public ServerStatus(Capacity capacity, CPU cpu, RAM ram, SystemInfo system) {
        this.capacity = capacity;
        this.cpu = cpu;
        this.ram = ram;
        this.system = system;
    }

    public static ServerStatusBuilder builder() {
        return new ServerStatusBuilder();
    }

    public static class ServerStatusBuilder {
        private Capacity capacity;
        private CPU cpu;
        private RAM ram;
        private SystemInfo system;

        public ServerStatusBuilder capacity(Capacity capacity) {
            this.capacity = capacity;
            return this;
        }

        public ServerStatusBuilder cpu(CPU cpu) {
            this.cpu = cpu;
            return this;
        }

        public ServerStatusBuilder ram(RAM ram) {
            this.ram = ram;
            return this;
        }

        public ServerStatusBuilder system(SystemInfo system) {
            this.system = system;
            return this;
        }

        public ServerStatus build() {
            return new ServerStatus(capacity, cpu, ram, system);
        }
    }

    // Getters and Setters
    public Capacity getCapacity() {
        return capacity;
    }

    public void setCapacity(Capacity capacity) {
        this.capacity = capacity;
    }

    public CPU getCpu() {
        return cpu;
    }

    public void setCpu(CPU cpu) {
        this.cpu = cpu;
    }

    public RAM getRam() {
        return ram;
    }

    public void setRam(RAM ram) {
        this.ram = ram;
    }

    public SystemInfo getSystem() {
        return system;
    }

    public void setSystem(SystemInfo system) {
        this.system = system;
    }
}
