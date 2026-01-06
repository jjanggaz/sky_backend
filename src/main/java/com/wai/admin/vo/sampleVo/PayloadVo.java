package com.wai.admin.vo.sampleVo;

import java.util.List;

public class PayloadVo {
	
    private String reportTitle;
    private CompanyVo company;
    private List<HouseItemVo> house;

    // Constructors
    public PayloadVo() {
    }

    // Getters and Setters
    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public CompanyVo getCompany() {
        return company;
    }

    public void setCompany(CompanyVo company) {
        this.company = company;
    }

    public List<HouseItemVo> getHouse() {
        return house;
    }

    public void setHouse(List<HouseItemVo> house) {
        this.house = house;
    }
}
