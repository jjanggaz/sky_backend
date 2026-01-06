package com.wai.admin.controller.reports.calculate;

import java.util.List;

public class FileTabInfo {
    private String fileName;
    private String processId;
    private String processNo;
    private String excelUrl;
    private int duplicateCnt;
    private List<String> sheetTabName;

    public FileTabInfo() {}

    public FileTabInfo(String fileName, List<String> sheetTabName) {
        this.fileName = fileName;
        this.sheetTabName = sheetTabName;
    }

    public FileTabInfo(String fileName, String processId, String processNo, String excelUrl, int duplicateCnt, List<String> sheetTabName) {
        this.fileName = fileName;
        this.processId = processId;
        this.processNo = processNo;
        this.excelUrl = excelUrl;
        this.duplicateCnt = duplicateCnt;
        this.sheetTabName = sheetTabName;
    }

    // Getters and Setters
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getProcessNo() {
        return processNo;
    }

    public void setProcessNo(String processNo) {
        this.processNo = processNo;
    }

    public String getExcelUrl() {
        return excelUrl;
    }

    public void setExcelUrl(String excelUrl) {
        this.excelUrl = excelUrl;
    }

    public int getDuplicateCnt() {
        return duplicateCnt;
    }

    public void setDuplicateCnt(int duplicateCnt) {
        this.duplicateCnt = duplicateCnt;
    }

    public List<String> getSheetTabName() {
        return sheetTabName;
    }

    public void setSheetTabName(List<String> sheetTabName) {
        this.sheetTabName = sheetTabName;
    }
}
