package com.wai.admin.vo.sampleVo;

import java.util.List;

public class CompanyVo {
	
    private String name;
    private String address;
    private List<ItemVo> items;

    // Constructors
    public CompanyVo() {
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<ItemVo> getItems() {
        return items;
    }

    public void setItems(List<ItemVo> items) {
        this.items = items;
    }
}
