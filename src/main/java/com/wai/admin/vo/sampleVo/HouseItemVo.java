package com.wai.admin.vo.sampleVo;

public class HouseItemVo {
	
    private String info_name;
    private Double info_price;
    private Integer info_qty;
    private String info_date;

    // Constructors
    public HouseItemVo() {
    }

    // Getters and Setters
    public String getInfo_name() {
        return info_name;
    }

    public void setInfo_name(String info_name) {
        this.info_name = info_name;
    }

    public Double getInfo_price() {
        return info_price;
    }

    public void setInfo_price(Double info_price) {
        this.info_price = info_price;
    }

    public Integer getInfo_qty() {
        return info_qty;
    }

    public void setInfo_qty(Integer info_qty) {
        this.info_qty = info_qty;
    }

    public String getInfo_date() {
        return info_date;
    }

    public void setInfo_date(String info_date) {
        this.info_date = info_date;
    }

    public Double getInfo_amount() {
        if (info_price == null || info_qty == null) return 0d;
        return info_price * info_qty;
    }
}