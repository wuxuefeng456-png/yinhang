package com.bankops.service;

import com.bankops.dto.Requests;
import com.bankops.model.GovernanceRecord;
import com.bankops.model.ManagementModule;
import com.bankops.model.SpecialistDuty;
import com.bankops.repository.GovernanceRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GovernanceRecordService {
    private static final Map<ManagementModule, Map<String, WorkflowStep>> WORKFLOWS = Map.of(
            ManagementModule.ARCHITECTURE, Map.of(
                    "待评审", step("评审中", SpecialistDuty.REVIEW, "开始评审"),
                    "评审中", step("已通过", SpecialistDuty.APPROVE, "审批通过"),
                    "已通过", step("运行中", SpecialistDuty.OPERATE, "上线运行"),
                    "运行中", step("已下线", SpecialistDuty.CLOSE, "下线归档")),
            ManagementModule.OUTSOURCING, Map.of(
                    "待审批", step("服务中", SpecialistDuty.APPROVE, "审批通过"),
                    "待评分", step("评分中", SpecialistDuty.REVIEW, "开始评价"),
                    "评分中", step("已完成", SpecialistDuty.APPROVE, "确认评价"),
                    "服务中", step("已离场", SpecialistDuty.CLOSE, "确认离场")),
            ManagementModule.PROJECT, Map.of(
                    "待立项", step("已立项", SpecialistDuty.APPROVE, "批准立项"),
                    "已立项", step("实施中", SpecialistDuty.OPERATE, "开始实施"),
                    "实施中", step("待验收", SpecialistDuty.OPERATE, "提交验收"),
                    "待验收", step("已结项", SpecialistDuty.CLOSE, "验收结项")),
            ManagementModule.REQUIREMENT, Map.of(
                    "待评审", step("已排期", SpecialistDuty.REVIEW, "确认需求"),
                    "已排期", step("开发中", SpecialistDuty.OPERATE, "开始实施"),
                    "开发中", step("待验收", SpecialistDuty.OPERATE, "提交验收"),
                    "待验收", step("已关闭", SpecialistDuty.CLOSE, "验收关闭")),
            ManagementModule.CHANGE_RELEASE, Map.of(
                    "待评审", step("会签中", SpecialistDuty.REVIEW, "发起会签"),
                    "会签中", step("审批中", SpecialistDuty.REVIEW, "完成会签"),
                    "审批中", step("待发布", SpecialistDuty.APPROVE, "审批发布"),
                    "待发布", step("验证中", SpecialistDuty.OPERATE, "执行发布"),
                    "验证中", step("已完成", SpecialistDuty.CLOSE, "验证关闭")),
            ManagementModule.ASSET_RESOURCE, Map.of(
                    "待审批", step("已批准", SpecialistDuty.APPROVE, "批准申请"),
                    "已批准", step("已分配", SpecialistDuty.OPERATE, "分配资源"),
                    "已分配", step("使用中", SpecialistDuty.OPERATE, "确认领用"),
                    "使用中", step("已完成", SpecialistDuty.CLOSE, "完成归档")),
            ManagementModule.PERSONNEL, Map.of(
                    "待确认", step("已确认", SpecialistDuty.REVIEW, "确认安排"),
                    "已确认", step("执行中", SpecialistDuty.OPERATE, "开始执行"),
                    "执行中", step("已完成", SpecialistDuty.CLOSE, "完成确认")),
            ManagementModule.DOCUMENT, Map.of(
                    "草稿", step("待发布", SpecialistDuty.REGISTER, "提交发布"),
                    "待发布", step("已发布", SpecialistDuty.APPROVE, "批准发布"),
                    "已发布", step("复审中", SpecialistDuty.REVIEW, "发起复审"),
                    "复审中", step("已归档", SpecialistDuty.CLOSE, "复审归档")),
            ManagementModule.RISK_COMPLIANCE, Map.of(
                    "已发现", step("整改中", SpecialistDuty.OPERATE, "开始整改"),
                    "整改中", step("待复核", SpecialistDuty.REVIEW, "提交复核"),
                    "待复核", step("已关闭", SpecialistDuty.APPROVE, "复核关闭")));

    private static final Map<ManagementModule, String> REJECT_STATUSES = Map.of(
            ManagementModule.ARCHITECTURE, "已驳回",
            ManagementModule.OUTSOURCING, "已驳回",
            ManagementModule.PROJECT, "已驳回",
            ManagementModule.REQUIREMENT, "已驳回",
            ManagementModule.CHANGE_RELEASE, "已驳回",
            ManagementModule.ASSET_RESOURCE, "已驳回",
            ManagementModule.RISK_COMPLIANCE, "整改驳回");

    private final GovernanceRecordRepository repository;

    public GovernanceRecordService(GovernanceRecordRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<GovernanceRecord> find(ManagementModule module, String department, String status,
                                       String category, String keyword, LocalDate fromDate, LocalDate toDate) {
        String departmentFilter = clean(department).toLowerCase(Locale.ROOT);
        String statusFilter = clean(status).toLowerCase(Locale.ROOT);
        String categoryFilter = clean(category).toLowerCase(Locale.ROOT);
        String keywordFilter = clean(keyword).toLowerCase(Locale.ROOT);
        return repository.findAllByModuleOrderByUpdatedAtDesc(module).stream()
                .filter(record -> departmentFilter.isEmpty() || contains(record.getDepartment(), departmentFilter))
                .filter(record -> statusFilter.isEmpty() || contains(record.getStatus(), statusFilter))
                .filter(record -> categoryFilter.isEmpty() || contains(record.getCategory(), categoryFilter))
                .filter(record -> fromDate == null || (record.getPlannedDate() != null && !record.getPlannedDate().isBefore(fromDate)))
                .filter(record -> toDate == null || (record.getPlannedDate() != null && !record.getPlannedDate().isAfter(toDate)))
                .filter(record -> keywordFilter.isEmpty()
                        || contains(record.getRecordNo(), keywordFilter)
                        || contains(record.getTitle(), keywordFilter)
                        || contains(record.getOwner(), keywordFilter)
                        || contains(record.getDetails(), keywordFilter))
                .toList();
    }

    @Transactional
    public GovernanceRecord create(ManagementModule module, Requests.GovernanceRecordRequest request, String creator) {
        GovernanceRecord record = new GovernanceRecord();
        record.setModule(module);
        record.setRecordNo(module.prefix() + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + ThreadLocalRandom.current().nextInt(100, 1000));
        record.setCreatedBy(creator);
        apply(record, request);
        return repository.save(record);
    }

    @Transactional
    public GovernanceRecord update(ManagementModule module, Long id, Requests.GovernanceRecordRequest request) {
        GovernanceRecord record = findRecord(module, id);
        apply(record, request);
        return repository.save(record);
    }

    @Transactional
    public GovernanceRecord updateStatus(ManagementModule module, Long id, String status) {
        GovernanceRecord record = findRecord(module, id);
        record.setStatus(status.trim());
        return repository.save(record);
    }

    @Transactional(readOnly = true)
    public Map<String, WorkflowStep> workflow(ManagementModule module) {
        return WORKFLOWS.getOrDefault(module, Map.of());
    }

    @Transactional(readOnly = true)
    public SpecialistDuty requiredDuty(ManagementModule module, Long id) {
        GovernanceRecord record = findRecord(module, id);
        WorkflowStep step = workflow(module).get(record.getStatus());
        if (step == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "当前状态没有可推进的流程");
        return step.duty();
    }

    @Transactional
    public GovernanceRecord advance(ManagementModule module, Long id) {
        GovernanceRecord record = findRecord(module, id);
        WorkflowStep step = workflow(module).get(record.getStatus());
        if (step == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "当前状态没有可推进的流程");
        record.setStatus(step.nextStatus());
        return repository.save(record);
    }

    @Transactional
    public GovernanceRecord review(ManagementModule module, Long id, Requests.GovernanceReviewRequest request) {
        GovernanceRecord record = findRecord(module, id);
        record.setScore(request.score());
        record.setCompliant(request.compliant() == null || request.compliant());
        record.setDetails(request.details().trim());
        return repository.save(record);
    }

    @Transactional
    public GovernanceRecord reject(ManagementModule module, Long id, String reason) {
        GovernanceRecord record = findRecord(module, id);
        String rejectStatus = REJECT_STATUSES.get(module);
        if (rejectStatus == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "当前模块不支持驳回操作");
        record.setStatus(rejectStatus);
        String original = clean(record.getDetails());
        record.setDetails((original.isEmpty() ? "" : original + "\n") + "[驳回原因] " + reason.trim());
        return repository.save(record);
    }

    @Transactional
    public void delete(ManagementModule module, Long id) {
        GovernanceRecord record = findRecord(module, id);
        repository.delete(record);
    }

    @Transactional(readOnly = true)
    public String exportCsv(ManagementModule module, String department, String status, String category, String keyword,
                            LocalDate fromDate, LocalDate toDate) {
        StringBuilder csv = new StringBuilder("记录编号,名称,业务类型,部门,分类,状态,负责人,计划日期,优先级,风险等级,版本,评分,合规,说明\r\n");
        for (GovernanceRecord record : find(module, department, status, category, keyword, fromDate, toDate)) {
            csv.append(csv(record.getRecordNo())).append(',')
                    .append(csv(record.getTitle())).append(',')
                    .append(csv(record.getRecordType())).append(',')
                    .append(csv(record.getDepartment())).append(',')
                    .append(csv(record.getCategory())).append(',')
                    .append(csv(record.getStatus())).append(',')
                    .append(csv(record.getOwner())).append(',')
                    .append(csv(record.getPlannedDate())).append(',')
                    .append(csv(record.getPriority())).append(',')
                    .append(csv(record.getRiskLevel())).append(',')
                    .append(csv(record.getVersionNo())).append(',')
                    .append(csv(record.getScore())).append(',')
                    .append(record.getCompliant() ? "是" : "否").append(',')
                    .append(csv(record.getDetails())).append("\r\n");
        }
        return csv.toString();
    }

    private GovernanceRecord findRecord(ManagementModule module, Long id) {
        GovernanceRecord record = repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "业务记录不存在"));
        if (record.getModule() != module) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "业务记录不存在");
        }
        return record;
    }

    private void apply(GovernanceRecord record, Requests.GovernanceRecordRequest request) {
        record.setTitle(request.title().trim());
        record.setRecordType(request.recordType().trim());
        record.setDepartment(request.department().trim());
        record.setCategory(clean(request.category()));
        record.setStatus(request.status().trim());
        record.setOwner(request.owner().trim());
        record.setPlannedDate(request.plannedDate());
        record.setPriority(clean(request.priority()));
        record.setRiskLevel(clean(request.riskLevel()));
        record.setVersionNo(clean(request.versionNo()));
        record.setScore(request.score());
        record.setCompliant(request.compliant() == null || request.compliant());
        record.setDetails(clean(request.details()));
    }

    private boolean contains(Object value, String filter) {
        return value != null && value.toString().toLowerCase(Locale.ROOT).contains(filter);
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }

    private String csv(Object value) {
        String text = value == null ? "" : value.toString();
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private static WorkflowStep step(String nextStatus, SpecialistDuty duty, String actionLabel) {
        return new WorkflowStep(nextStatus, duty, actionLabel);
    }

    public record WorkflowStep(String nextStatus, SpecialistDuty duty, String actionLabel) {}
}
