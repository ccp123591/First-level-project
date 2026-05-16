package com.fitcoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FitCoach 后端启动类
 */
@SpringBootApplication
public class FitCoachApplication {
    public static void main(String[] args) {
        SpringApplication.run(FitCoachApplication.class, args);
        System.out.println("""

            ╔═══════════════════════════════════════════════════════╗
            ║  FitCoach Backend v3.0 启动成功！                      ║
            ║                                                        ║
            ║  API 文档:    http://localhost:8080/swagger-ui.html   ║
            ║  H2 控制台:   http://localhost:8080/h2-console        ║
            ║  前端代理:    http://localhost:5173 → /api            ║
            ╚═══════════════════════════════════════════════════════╝
            """);
    }
}
