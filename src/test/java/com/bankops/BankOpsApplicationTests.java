package com.bankops;

import com.bankops.model.AccountStatus;
import com.bankops.model.Role;
import com.bankops.model.UserAccount;
import com.bankops.repository.UserAccountRepository;
import com.bankops.repository.WorkOrderRepository;
import com.bankops.service.PasswordService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bankops-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class BankOpsApplicationTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserAccountRepository users;
    @Autowired
    private WorkOrderRepository orders;
    @Autowired
    private PasswordService passwordService;

    @Test
    void authenticationRoleAndLockingFlowWorks() throws Exception {
        assertThat(users.existsByUsernameIgnoreCase("opsengineer")).isFalse();
        String adminToken = loginAndReadToken("bankadmin", "Admin@123", "ADMIN");
        String memberToken = loginAndReadToken("member01", "Member@123", "MEMBER");

        mockMvc.perform(get("/api/admin/users").header("X-Auth-Token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());

        mockMvc.perform(get("/api/admin/users").header("X-Auth-Token", memberToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("该功能仅管理员可用"));

        mockMvc.perform(post("/api/work-orders")
                        .header("X-Auth-Token", memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"无权创建\",\"category\":\"服务请求\",\"priority\":\"低\",\"status\":\"PENDING\",\"systemName\":\"测试系统\",\"assignee\":\"普通成员\",\"description\":\"\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/work-orders/my").header("X-Auth-Token", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.status == 'PENDING')]").isNotEmpty());

        Long pendingOrderId = orders.findAll().stream()
                .filter(order -> order.getStatus() == com.bankops.model.WorkOrderStatus.PENDING)
                .findFirst().orElseThrow().getId();
        mockMvc.perform(put("/api/work-orders/{id}/start", pendingOrderId)
                        .header("X-Auth-Token", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.assignee").value("member01"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newmember\",\"displayName\":\"新成员\",\"password\":\"Member@456\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("公开注册已关闭，请联系管理员创建账号"));

        mockMvc.perform(post("/api/admin/users")
                        .header("X-Auth-Token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newmember\",\"displayName\":\"新成员\",\"password\":\"Member@456\",\"role\":\"MEMBER\",\"specialtyModule\":null,\"specialistDuty\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.specialtyModule").isEmpty());
        String newMemberToken = loginAndReadToken("newmember", "Member@456", "MEMBER");

        mockMvc.perform(put("/api/work-orders/{id}/complete", pendingOrderId)
                        .header("X-Auth-Token", newMemberToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只能完成自己领取的工单"));

        mockMvc.perform(put("/api/work-orders/{id}/complete", pendingOrderId)
                        .header("X-Auth-Token", memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.assignee").value("member01"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin'--\",\"password\":\"Bad@1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("非法账号")));

        PasswordService.PasswordValue password = passwordService.create("LockTest@123");
        UserAccount lockUser = new UserAccount();
        lockUser.setUsername("locktester");
        lockUser.setDisplayName("锁定测试账号");
        lockUser.setRole(Role.MEMBER);
        lockUser.setStatus(AccountStatus.ACTIVE);
        lockUser.setPasswordHash(password.hash());
        lockUser.setPasswordSalt(password.salt());
        users.save(lockUser);

        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"locktester\",\"password\":\"Wrong@123\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"locktester\",\"password\":\"Wrong@123\"}"))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("自动锁定")));

        assertThat(users.findByUsernameIgnoreCase("locktester").orElseThrow().getStatus())
                .isEqualTo(AccountStatus.LOCKED);
    }

    @Test
    void governanceModulesSupportAdminCrudFilteringWorkflowAndExport() throws Exception {
        String adminToken = loginAndReadToken("bankadmin", "Admin@123", "ADMIN");
        String memberToken = loginAndReadToken("member01", "Member@123", "MEMBER");

        mockMvc.perform(get("/api/governance/ARCHITECTURE").header("X-Auth-Token", memberToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("没有该管理模块的岗位权限"));

        mockMvc.perform(get("/api/governance/ARCHITECTURE").header("X-Auth-Token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].module").value("ARCHITECTURE"));

        String createdJson = mockMvc.perform(post("/api/governance/PROJECT")
                        .header("X-Auth-Token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"项目模块自动化验证\",\"recordType\":\"项目立项\",\"department\":\"科技部\",\"category\":\"2026年度\",\"status\":\"待立项\",\"owner\":\"测试管理员\",\"plannedDate\":\"2026-11-30\",\"priority\":\"高\",\"riskLevel\":\"中\",\"versionNo\":\"V1.0\",\"score\":10,\"compliant\":true,\"details\":\"测试项目立项、筛选、审批和导出\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value("PROJECT"))
                .andExpect(jsonPath("$.status").value("待立项"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createdJson).get("id").asLong();

        mockMvc.perform(get("/api/governance/PROJECT")
                        .param("department", "科技部")
                        .param("status", "待立项")
                        .param("fromDate", "2026-09-10")
                        .header("X-Auth-Token", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == '项目模块自动化验证')]").isNotEmpty());

        mockMvc.perform(put("/api/governance/PROJECT/{id}/status", id)
                        .header("X-Auth-Token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"已立项\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("已立项"));

        mockMvc.perform(get("/api/governance/PROJECT/export")
                        .header("X-Auth-Token", adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/governance/PROJECT/{id}", id)
                        .header("X-Auth-Token", adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void specialistsCanOnlyOperateTheirAssignedModuleAndWorkflowDuty() throws Exception {
        String adminToken = loginAndReadToken("bankadmin", "Admin@123", "ADMIN");
        String entryToken = loginAndReadToken("arch_entry", "Special@123", "MEMBER");
        String reviewToken = loginAndReadToken("arch_review", "Special@123", "MEMBER");
        String approveToken = loginAndReadToken("arch_approve", "Special@123", "MEMBER");

        mockMvc.perform(get("/api/governance/ARCHITECTURE").header("X-Auth-Token", reviewToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/governance/PROJECT").header("X-Auth-Token", reviewToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("没有该管理模块的岗位权限"));

        String requestBody = "{\"title\":\"专员分工流程验证\",\"recordType\":\"架构评审\",\"department\":\"科技部\",\"category\":\"Java / 微服务\",\"status\":\"待评审\",\"owner\":\"架构台账专员\",\"plannedDate\":\"2026-09-20\",\"priority\":\"高\",\"riskLevel\":\"中\",\"versionNo\":\"V1.0\",\"score\":null,\"compliant\":true,\"details\":\"等待架构评审专员处理\"}";
        String createdJson = mockMvc.perform(post("/api/governance/ARCHITECTURE")
                        .header("X-Auth-Token", entryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("待评审"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createdJson).get("id").asLong();

        mockMvc.perform(post("/api/governance/ARCHITECTURE")
                        .header("X-Auth-Token", reviewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前操作需要“台账录入”岗位"));

        mockMvc.perform(put("/api/governance/ARCHITECTURE/{id}/advance", id)
                        .header("X-Auth-Token", reviewToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("评审中"));

        mockMvc.perform(put("/api/governance/ARCHITECTURE/{id}/review", id)
                        .header("X-Auth-Token", reviewToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":86,\"compliant\":true,\"details\":\"技术方案符合规范，建议审批通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(86));

        mockMvc.perform(put("/api/governance/ARCHITECTURE/{id}/advance", id)
                        .header("X-Auth-Token", reviewToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("当前操作需要“审批决策”岗位"));

        mockMvc.perform(put("/api/governance/ARCHITECTURE/{id}/advance", id)
                        .header("X-Auth-Token", approveToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("已通过"));

        mockMvc.perform(delete("/api/governance/ARCHITECTURE/{id}", id)
                        .header("X-Auth-Token", adminToken))
                .andExpect(status().isOk());
    }

    private String loginAndReadToken(String username, String password, String expectedRole) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value(expectedRole))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }
}
