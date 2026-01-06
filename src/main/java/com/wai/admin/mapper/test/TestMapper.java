package com.wai.admin.mapper.test;

import com.wai.admin.vo.test.Sky1Vo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Test 관련 Mapper 인터페이스
 */
@Mapper
public interface TestMapper {

    /**
     * sky1 테이블에서 모든 데이터 조회
     * @return Sky1Vo 리스트
     */
    List<Sky1Vo> selectSky1List();
}

