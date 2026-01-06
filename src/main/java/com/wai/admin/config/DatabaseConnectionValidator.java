package com.wai.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 애플리케이션 시작 시 데이터베이스 연결을 확인하는 컴포넌트
 */
@Component
@Order(1) // 다른 ApplicationRunner보다 먼저 실행
public class DatabaseConnectionValidator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionValidator.class);
    
    private final DataSource dataSource;

    public DatabaseConnectionValidator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("데이터베이스 연결 확인을 시작합니다...");
        
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null && !connection.isClosed()) {
                String url = connection.getMetaData().getURL();
                String databaseProductName = connection.getMetaData().getDatabaseProductName();
                String databaseProductVersion = connection.getMetaData().getDatabaseProductVersion();
                
                logger.info("========================================");
                logger.info("데이터베이스 연결 성공!");
                logger.info("URL: {}", url);
                logger.info("데이터베이스: {} {}", databaseProductName, databaseProductVersion);
                logger.info("========================================");
            } else {
                throw new SQLException("데이터베이스 연결이 null이거나 이미 닫혔습니다.");
            }
        } catch (SQLException e) {
            logger.error("========================================");
            logger.error("데이터베이스 연결 실패!");
            logger.error("오류 메시지: {}", e.getMessage());
            logger.error("========================================");
            throw new RuntimeException("데이터베이스 연결에 실패했습니다. 애플리케이션을 시작할 수 없습니다.", e);
        }
    }
}

