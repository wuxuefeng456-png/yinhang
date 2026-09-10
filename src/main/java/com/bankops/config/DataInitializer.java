package com.bankops.config;

import com.bankops.model.*;
import com.bankops.repository.AssetRepository;
import com.bankops.repository.GovernanceRecordRepository;
import com.bankops.repository.UserAccountRepository;
import com.bankops.repository.WorkOrderRepository;
import com.bankops.service.PasswordService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initializeData(UserAccountRepository users,
                                     WorkOrderRepository orders,
                                     AssetRepository assets,
                                     GovernanceRecordRepository governance,
                                     PasswordService passwordService,
                                     JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.update("DELETE FROM user_account WHERE account_role = 'ENGINEER'");
            jdbcTemplate.update("UPDATE work_order SET assignee = 'member01' WHERE assignee = '运维工程师'");
            jdbcTemplate.update("UPDATE work_order SET assignee = 'member01' WHERE assignee = '普通成员' AND status <> 'PENDING'");
            jdbcTemplate.update("UPDATE work_order SET assignee = '' WHERE assignee = '普通成员' AND status = 'PENDING'");
            jdbcTemplate.update("UPDATE asset SET owner = '平台管理员' WHERE owner = '运维工程师'");
            if (!users.existsByUsernameIgnoreCase("bankadmin")) {
                users.save(createUser("bankadmin", "系统管理员", "Admin@123", Role.ADMIN, passwordService));
            }
            if (!users.existsByUsernameIgnoreCase("member01")) {
                users.save(createUser("member01", "普通成员", "Member@123", Role.MEMBER, passwordService));
            }
            ensureSpecialist(users, passwordService, "arch_entry", "架构台账专员", ManagementModule.ARCHITECTURE, SpecialistDuty.REGISTER);
            ensureSpecialist(users, passwordService, "arch_review", "架构评审专员", ManagementModule.ARCHITECTURE, SpecialistDuty.REVIEW);
            ensureSpecialist(users, passwordService, "arch_approve", "架构审批专员", ManagementModule.ARCHITECTURE, SpecialistDuty.APPROVE);
            ensureSpecialist(users, passwordService, "outsource_eval", "外包评价专员", ManagementModule.OUTSOURCING, SpecialistDuty.REVIEW);
            ensureSpecialist(users, passwordService, "project_owner", "项目进度专员", ManagementModule.PROJECT, SpecialistDuty.OPERATE);
            ensureSpecialist(users, passwordService, "req_confirm", "需求确认专员", ManagementModule.REQUIREMENT, SpecialistDuty.REVIEW);
            ensureSpecialist(users, passwordService, "change_approve", "变更审批专员", ManagementModule.CHANGE_RELEASE, SpecialistDuty.APPROVE);
            ensureSpecialist(users, passwordService, "resource_approve", "资源审批专员", ManagementModule.ASSET_RESOURCE, SpecialistDuty.APPROVE);
            ensureSpecialist(users, passwordService, "team_schedule", "排班执行专员", ManagementModule.PERSONNEL, SpecialistDuty.OPERATE);
            ensureSpecialist(users, passwordService, "document_review", "制度复审专员", ManagementModule.DOCUMENT, SpecialistDuty.REVIEW);
            ensureSpecialist(users, passwordService, "risk_verify", "风险复核专员", ManagementModule.RISK_COMPLIANCE, SpecialistDuty.APPROVE);
            if (orders.count() == 0) {
                orders.save(order("OPS-20260910-001", "核心支付网关延迟排查", "故障事件", "高", WorkOrderStatus.PROCESSING,
                        "统一支付平台", "member01", "交易峰值时段响应时间升高，正在检查应用线程池与数据库连接池。", "bankadmin"));
                orders.save(order("OPS-20260910-002", "灾备切换演练前置检查", "变更任务", "中", WorkOrderStatus.PENDING,
                        "灾备管理平台", "", "核对复制延迟、路由策略及回切步骤。", "bankadmin"));
                orders.save(order("OPS-20260909-003", "报表服务器磁盘扩容", "服务请求", "低", WorkOrderStatus.RESOLVED,
                        "监管报送系统", "member01", "数据盘已扩容并完成文件系统检查。", "bankadmin"));
                orders.save(order("OPS-20260908-004", "网银证书到期提醒处理", "安全事件", "高", WorkOrderStatus.CLOSED,
                        "企业网银", "member01", "新证书部署完成，双向TLS连接验证通过。", "bankadmin"));
            }
            if (assets.count() == 0) {
                assets.save(asset("APP-PAY-01", "支付应用节点01", "应用服务器", "10.20.1.11", "生产", AssetStatus.ONLINE, "平台管理员"));
                assets.save(asset("DB-CORE-01", "核心业务主库", "数据库", "10.20.2.21", "生产", AssetStatus.ONLINE, "数据库组"));
                assets.save(asset("GW-EBANK-02", "企业网银网关02", "安全网关", "10.20.3.32", "生产", AssetStatus.MAINTENANCE, "网络组"));
                assets.save(asset("APP-DR-01", "灾备应用节点01", "应用服务器", "10.30.1.11", "灾备", AssetStatus.ONLINE, "平台管理员"));
                assets.save(asset("MON-UAT-01", "监控验证节点", "监控节点", "10.40.1.18", "测试", AssetStatus.OFFLINE, "平台管理员"));
            }
            if (governance.count() == 0) {
                governance.save(governance("ARC-2026-001", ManagementModule.ARCHITECTURE, "核心账务平台架构", "架构台账", "科技部", "Java / Spring Cloud", "运行中", "王架构", LocalDate.of(2026, 8, 15), "高", "低", "V3.2", 92, true, "分布式部署，数据库主备，已纳入年度架构复审。"));
                governance.save(governance("ARC-2026-002", ManagementModule.ARCHITECTURE, "统一支付平台容器化评审", "架构评审", "电子银行部", "Kubernetes / 微服务", "待评审", "李专家", LocalDate.of(2026, 9, 18), "中", "中", "V1.0", null, false, "待补充跨机房容灾与容量影响分析。"));
                governance.save(governance("OUT-2026-001", ManagementModule.OUTSOURCING, "华东金融科技服务商", "外包商台账", "采购管理部", "应用运维", "服务中", "赵经理", LocalDate.of(2026, 12, 31), "中", "低", "2026合同", 88, true, "资质和保密协议均在有效期内。"));
                governance.save(governance("OUT-2026-002", ManagementModule.OUTSOURCING, "三季度外包服务评价", "外包评价", "科技部", "季度评价", "待评分", "陈主管", LocalDate.of(2026, 9, 25), "高", "中", "2026Q3", null, true, "已指定应用、网络、安全三个评价人。"));
                governance.save(governance("PRJ-2026-001", ManagementModule.PROJECT, "新一代监控平台建设", "项目进度", "科技部", "基础设施", "实施中", "周项目", LocalDate.of(2026, 12, 20), "高", "中", "里程碑3", 65, true, "采集端部署完成，正在进行告警规则迁移。"));
                governance.save(governance("REQ-2026-001", ManagementModule.REQUIREMENT, "手机银行登录链路监控", "需求登记", "电子银行部", "监控增强", "待评审", "孙产品", LocalDate.of(2026, 10, 10), "高", "低", "R1", null, true, "增加登录耗时、失败率和地区分布监控。"));
                governance.save(governance("CHG-2026-001", ManagementModule.CHANGE_RELEASE, "核心数据库季度补丁升级", "变更申请", "数据中心", "数据库变更", "审批中", "钱运维", LocalDate.of(2026, 9, 20), "高", "中", "补丁12.4", null, true, "已提交回退脚本，等待风险会签。"));
                governance.save(governance("RES-2026-001", ManagementModule.ASSET_RESOURCE, "监管报送系统存储扩容", "资源申请", "数据管理部", "存储资源", "待审批", "吴申请", LocalDate.of(2026, 9, 22), "中", "低", "扩容2TB", null, true, "申请生产存储2TB并同步扩充备份空间。"));
                governance.save(governance("PER-2026-001", ManagementModule.PERSONNEL, "国庆期间运维值班安排", "值班排班", "科技部", "AB岗", "待确认", "郑主管", LocalDate.of(2026, 9, 28), "高", "低", "2026国庆", null, true, "核心、支付、网络和安全岗位均配置AB角。"));
                governance.save(governance("DOC-2026-001", ManagementModule.DOCUMENT, "生产变更管理办法", "制度发布", "风险管理部", "管理制度", "复审中", "冯合规", LocalDate.of(2026, 10, 31), "中", "低", "V2.1", null, true, "年度复审，需补充自动化发布和紧急变更条款。"));
                governance.save(governance("RSK-2026-001", ManagementModule.RISK_COMPLIANCE, "灾备切换演练整改项", "问题整改", "数据中心", "业务连续性", "整改中", "褚经理", LocalDate.of(2026, 9, 30), "高", "高", "整改批次1", 40, false, "异地灾备切换耗时超过目标，需要优化数据校验流程。"));
            }
        };
    }

    private UserAccount createUser(String username, String displayName, String rawPassword, Role role, PasswordService service) {
        return createUser(username, displayName, rawPassword, role, null, null, service);
    }

    private UserAccount createUser(String username, String displayName, String rawPassword, Role role,
                                   ManagementModule specialtyModule, SpecialistDuty specialistDuty,
                                   PasswordService service) {
        PasswordService.PasswordValue password = service.create(rawPassword);
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setSpecialtyModule(specialtyModule);
        user.setSpecialistDuty(specialistDuty);
        user.setStatus(AccountStatus.ACTIVE);
        user.setPasswordHash(password.hash());
        user.setPasswordSalt(password.salt());
        return user;
    }

    private void ensureSpecialist(UserAccountRepository users, PasswordService passwordService,
                                  String username, String displayName, ManagementModule module,
                                  SpecialistDuty duty) {
        UserAccount user = users.findByUsernameIgnoreCase(username)
                .orElseGet(() -> createUser(username, displayName, "Special@123", Role.MEMBER,
                        module, duty, passwordService));
        user.setRole(Role.MEMBER);
        user.setSpecialtyModule(module);
        user.setSpecialistDuty(duty);
        users.save(user);
    }

    private WorkOrder order(String no, String title, String category, String priority, WorkOrderStatus status,
                            String system, String assignee, String description, String creator) {
        WorkOrder order = new WorkOrder();
        order.setTicketNo(no);
        order.setTitle(title);
        order.setCategory(category);
        order.setPriority(priority);
        order.setStatus(status);
        order.setSystemName(system);
        order.setAssignee(assignee);
        order.setDescription(description);
        order.setCreatedBy(creator);
        return order;
    }

    private Asset asset(String code, String name, String type, String ip, String env, AssetStatus status, String owner) {
        Asset asset = new Asset();
        asset.setAssetCode(code);
        asset.setAssetName(name);
        asset.setAssetType(type);
        asset.setIpAddress(ip);
        asset.setEnvironment(env);
        asset.setStatus(status);
        asset.setOwner(owner);
        asset.setDescription("银行运维平台纳管资产");
        return asset;
    }

    private GovernanceRecord governance(String no, ManagementModule module, String title, String recordType,
                                        String department, String category, String status, String owner,
                                        LocalDate plannedDate, String priority, String riskLevel, String versionNo,
                                        Integer score, boolean compliant, String details) {
        GovernanceRecord record = new GovernanceRecord();
        record.setRecordNo(no);
        record.setModule(module);
        record.setTitle(title);
        record.setRecordType(recordType);
        record.setDepartment(department);
        record.setCategory(category);
        record.setStatus(status);
        record.setOwner(owner);
        record.setPlannedDate(plannedDate);
        record.setPriority(priority);
        record.setRiskLevel(riskLevel);
        record.setVersionNo(versionNo);
        record.setScore(score);
        record.setCompliant(compliant);
        record.setDetails(details);
        record.setCreatedBy("bankadmin");
        return record;
    }
}
