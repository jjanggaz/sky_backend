package com.wai.admin.vo.test;

/**
 * sky1 테이블 VO 클래스
 */
public class Sky1Vo {
    
    private Integer id;
    private String name;
    private String idAndName;

    // Constructors
    public Sky1Vo() {
    }

    public Sky1Vo(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdAndName() {
        return idAndName;
    }

    public void setIdAndName(String idAndName) {
        this.idAndName = idAndName;
    }

    @Override
    public String toString() {
        return "Sky1Vo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", idAndName='" + idAndName + '\'' +
                '}';
    }
}


