package com.wai.admin.vo.report.calculate;

import java.util.ArrayList;
import java.util.List;

/**
 * Process 정보를 저장하는 VO 클래스
 */
public class ProcessInfoVo {
    private String processId;
    private String processName;
    private List<String> processNoList;      // 중복된 process_id의 모든 processNo 저장
    private List<String> downloadUrlList;    // processNo별 download_url 저장

    public ProcessInfoVo(String processId, String processName, String processNo) {
        this.processId = processId;
        this.processName = processName;
        this.processNoList = new ArrayList<>();
        this.processNoList.add(processNo);
        this.downloadUrlList = new ArrayList<>();
    }

    public void addProcessNo(String processNo) {
        this.processNoList.add(processNo);
    }

    public void addDownloadUrl(String downloadUrl) {
        this.downloadUrlList.add(downloadUrl);
    }

    public String getProcessId() { return processId; }
    public String getProcessName() { return processName; }
    public List<String> getProcessNoList() { return processNoList; }
    public List<String> getDownloadUrlList() { return downloadUrlList; }
    public int getDuplicateCount() { return processNoList.size(); }

    @Override
    public String toString() {
        return "ProcessInfoVo{" +
                "processId='" + processId + '\'' +
                ", processName='" + processName + '\'' +
                ", processNoList=" + processNoList +
                ", downloadUrlList=" + downloadUrlList +
                ", duplicateCount=" + getDuplicateCount() +
                '}';
    }
}
