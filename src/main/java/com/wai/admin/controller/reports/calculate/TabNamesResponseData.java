package com.wai.admin.controller.reports.calculate;

import java.util.List;

public class TabNamesResponseData {
    private String project_id;
    private String unit_system_code;
    private List<FileTabInfo> tablist;

    public TabNamesResponseData() {}

    public TabNamesResponseData(String project_id, String unit_system_code, List<FileTabInfo> tablist) {
        this.project_id = project_id;
        this.unit_system_code = unit_system_code;
        this.tablist = tablist;
    }

    // Getters and Setters
    public String getProject_id() {
        return project_id;
    }

    public void setProject_id(String project_id) {
        this.project_id = project_id;
    }

    public String getUnit_system_code() {
        return unit_system_code;
    }

    public void setUnit_system_code(String unit_system_code) {
        this.unit_system_code = unit_system_code;
    }

    public List<FileTabInfo> getTablist() {
        return tablist;
    }

    public void setTablist(List<FileTabInfo> tablist) {
        this.tablist = tablist;
    }
}
