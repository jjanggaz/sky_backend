package com.wai.admin.vo.sampleVo;

public class ItemVo {
	
    private String name;
    private Double price;
    private Integer qty;
    private String date;

    // Constructors
    public ItemVo() {
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Double getAmount() {
        if (price == null || qty == null) return 0d;
        return price * qty;
    }
}