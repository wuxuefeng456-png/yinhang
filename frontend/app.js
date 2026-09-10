(function () {
    "use strict";

    var API_BASE = location.port === "8081" ? "" : "http://localhost:8081";
    var state = {
        token: localStorage.getItem("bankops_token") || "",
        user: null,
        currentView: "overview",
        taskFilter: "PENDING",
        orders: [],
        assets: [],
        users: [],
        governanceRecords: [],
        governanceWorkflow: {},
        governanceFilters: {}
    };

    var loginView = document.getElementById("loginView");
    var appView = document.getElementById("appView");
    var content = document.getElementById("content");
    var modalBackdrop = document.getElementById("modalBackdrop");
    var modalForm = document.getElementById("modalForm");
    var toastTimer;

    var pageMeta = {
        overview: ["运维总览", "OPERATIONS OVERVIEW"],
        tasks: ["我的待办", "DAILY WORK QUEUE"],
        orders: ["工单中心", "WORK ORDER CENTER"],
        assets: ["资产台账", "ASSET INVENTORY"],
        architecture: ["架构管理", "ARCHITECTURE GOVERNANCE"],
        outsourcing: ["外包管理", "OUTSOURCING GOVERNANCE"],
        projects: ["项目管理", "PROJECT PORTFOLIO"],
        requirements: ["需求管理", "REQUIREMENT PORTFOLIO"],
        changes: ["变更与发布", "CHANGE & RELEASE"],
        resources: ["资产与资源", "ASSET & RESOURCE"],
        personnel: ["人员与团队", "PEOPLE & TEAM"],
        documents: ["制度与文档", "POLICY & DOCUMENT"],
        risks: ["风险与合规", "RISK & COMPLIANCE"],
        users: ["账号管理", "IDENTITY & ACCESS"],
        audits: ["登录审计", "SECURITY AUDIT"]
    };

    var governanceConfigs = {
        architecture: {
            module: "ARCHITECTURE", title: "架构管理", categoryLabel: "技术栈 / 架构类型", dateLabel: "上线 / 评审日期",
            types: ["架构台账", "架构评审", "架构变更", "合规检查"],
            statuses: ["待评审", "评审中", "已通过", "运行中", "已下线", "已驳回"],
            departments: ["科技部", "电子银行部", "数据中心", "风险管理部"], rejectStatus: "已驳回",
            features: ["架构台账", "多条件筛选", "架构评审", "变更留痕", "合规标记", "统计导出"]
        },
        outsourcing: {
            module: "OUTSOURCING", title: "外包管理", categoryLabel: "服务范围 / 评价周期", dateLabel: "合同 / 评价日期",
            types: ["外包商台账", "外包人员", "外包项目", "外包评价", "风险台账"],
            statuses: ["待审批", "待评分", "评分中", "已完成", "服务中", "已离场", "黑名单", "已驳回"],
            departments: ["采购管理部", "科技部", "安全管理部", "数据中心"], rejectStatus: "已驳回",
            features: ["供应商台账", "人员入离场", "外包项目", "发起评价", "排名续约", "风险台账"]
        },
        projects: {
            module: "PROJECT", title: "项目管理", categoryLabel: "项目类型 / 年度", dateLabel: "计划完成日期",
            types: ["项目立项", "项目进度", "项目验收", "项目结项"],
            statuses: ["待立项", "已立项", "实施中", "待验收", "已结项", "已驳回"],
            departments: ["科技部", "电子银行部", "数据中心", "数据管理部"], rejectStatus: "已驳回",
            features: ["立项申请", "审批归档", "里程碑", "周报预警", "验收结项", "项目看板"]
        },
        requirements: {
            module: "REQUIREMENT", title: "需求管理", categoryLabel: "所属系统 / 需求类别", dateLabel: "计划上线日期",
            types: ["需求登记", "需求评审", "需求排期", "需求变更", "需求关闭"],
            statuses: ["待评审", "已排期", "开发中", "待验收", "已关闭", "已驳回"],
            departments: ["电子银行部", "科技部", "数据管理部", "运营管理部"], rejectStatus: "已驳回",
            features: ["需求登记", "需求评审", "优先级排序", "排期跟踪", "变更关闭", "需求统计"]
        },
        changes: {
            module: "CHANGE_RELEASE", title: "变更与发布管理", categoryLabel: "变更类型 / 发布窗口", dateLabel: "计划变更日期",
            types: ["变更申请", "评审会签", "发布审批", "变更验证", "回退记录"],
            statuses: ["待评审", "会签中", "审批中", "待发布", "验证中", "已完成", "已回退", "已驳回"],
            departments: ["科技部", "数据中心", "电子银行部", "安全管理部"], rejectStatus: "已驳回",
            features: ["变更申请", "评审会签", "窗口管控", "发布审批", "回退确认", "变更统计"]
        },
        resources: {
            module: "ASSET_RESOURCE", title: "资产与资源管理", categoryLabel: "资产 / 资源类型", dateLabel: "申请 / 盘点日期",
            types: ["硬件资产", "软件资产", "资源申请", "资产调拨", "资产报废", "资产盘点"],
            statuses: ["待审批", "已批准", "已分配", "使用中", "已完成", "已报废", "已驳回"],
            departments: ["数据中心", "科技部", "数据管理部", "电子银行部"], rejectStatus: "已驳回",
            features: ["资产台账", "领用调拨", "资产报废", "资源审批", "资产盘点", "资产报表"]
        },
        personnel: {
            module: "PERSONNEL", title: "人员与团队管理", categoryLabel: "岗位 / 技能类别", dateLabel: "生效 / 到期日期",
            types: ["人员信息", "组织岗位", "技能矩阵", "AB岗配置", "值班排班", "培训证照"],
            statuses: ["待确认", "已确认", "执行中", "已完成", "已到期"],
            departments: ["科技部", "数据中心", "安全管理部", "电子银行部"],
            features: ["人员信息", "组织岗位", "技能矩阵", "AB岗配置", "值班排班", "培训证照"]
        },
        documents: {
            module: "DOCUMENT", title: "制度与文档管理", categoryLabel: "制度 / 文档类别", dateLabel: "发布 / 复审日期",
            types: ["制度发布", "制度修订", "操作手册", "阅读确认", "到期复审"],
            statuses: ["草稿", "待发布", "已发布", "复审中", "已归档"],
            departments: ["风险管理部", "科技部", "安全管理部", "运营管理部"],
            features: ["制度发布", "版本管理", "修订记录", "文档库", "阅读确认", "到期提醒"]
        },
        risks: {
            module: "RISK_COMPLIANCE", title: "风险与合规管理", categoryLabel: "风险 / 监管类别", dateLabel: "整改期限",
            types: ["风险台账", "检查计划", "问题整改", "监管对照", "上报材料", "复核关闭"],
            statuses: ["已发现", "整改中", "待复核", "已关闭", "整改驳回"],
            departments: ["风险管理部", "安全管理部", "科技部", "数据中心"], rejectStatus: "整改驳回",
            features: ["风险台账", "检查计划", "整改跟踪", "监管对照", "上报材料", "整改闭环"]
        }
    };

    var governanceFormOptions = {
        ARCHITECTURE: {
            titles: ["核心系统架构台账", "应用系统架构评审", "架构变更申请", "技术规范合规检查"],
            categories: ["Java / Spring Cloud", "Kubernetes / 微服务", "主备数据库 / 高可用", "集中式架构", "分布式架构"],
            owners: ["架构台账专员", "架构评审专员", "架构审批专员", "科技部负责人", "系统负责人"],
            versions: ["V1.0", "V2.0", "V3.0", "年度复审"]
        },
        OUTSOURCING: {
            titles: ["外包商准入登记", "外包人员入场申请", "外包项目服务登记", "季度外包服务评价", "外包风险登记"],
            categories: ["应用运维", "网络运维", "安全服务", "软件开发", "季度评价"],
            owners: ["外包台账专员", "外包评价专员", "采购管理负责人", "项目负责人", "安全管理负责人"],
            versions: ["2026Q1", "2026Q2", "2026Q3", "2026Q4", "年度合同"]
        },
        PROJECT: {
            titles: ["基础设施建设项目", "应用系统改造项目", "监控平台建设项目", "数据治理项目"],
            categories: ["基础设施", "应用开发", "安全建设", "数据治理", "年度重点项目"],
            owners: ["项目立项专员", "项目进度专员", "项目审批负责人", "项目验收负责人", "科技部负责人"],
            versions: ["立项版", "里程碑1", "里程碑2", "里程碑3", "结项版"]
        },
        REQUIREMENT: {
            titles: ["业务功能新增需求", "系统优化需求", "监控增强需求", "监管报送需求", "安全整改需求"],
            categories: ["核心账务系统", "统一支付平台", "手机银行", "企业网银", "监管报送系统"],
            owners: ["需求登记专员", "需求确认专员", "需求实施专员", "需求验收专员", "业务部门负责人"],
            versions: ["R1", "R2", "R3", "月度版本", "紧急版本"]
        },
        CHANGE_RELEASE: {
            titles: ["应用版本发布申请", "数据库变更申请", "网络策略变更申请", "紧急生产变更申请"],
            categories: ["应用变更", "数据库变更", "网络变更", "配置变更", "紧急变更"],
            owners: ["变更登记专员", "变更评审专员", "变更审批专员", "发布执行专员", "变更验证专员"],
            versions: ["普通窗口", "月度窗口", "季度窗口", "节假日窗口", "紧急窗口"]
        },
        ASSET_RESOURCE: {
            titles: ["服务器资源申请", "存储资源申请", "账号权限申请", "资产调拨申请", "资产报废申请", "资产盘点任务"],
            categories: ["服务器", "存储", "数据库", "网络设备", "账号权限"],
            owners: ["资产台账专员", "资源审批专员", "资源分配专员", "资产验收专员", "数据中心负责人"],
            versions: ["申请批次1", "申请批次2", "2026Q3", "2026Q4", "年度盘点"]
        },
        PERSONNEL: {
            titles: ["人员信息登记", "岗位职责确认", "技能矩阵维护", "AB岗配置", "值班排班安排", "培训证照登记"],
            categories: ["应用运维岗", "数据库岗", "网络岗", "安全岗", "技能与证照"],
            owners: ["人员台账专员", "人员确认专员", "排班执行专员", "培训管理专员", "部门负责人"],
            versions: ["2026Q3", "2026Q4", "国庆值班", "年度培训", "年度复审"]
        },
        DOCUMENT: {
            titles: ["运维管理制度", "生产变更管理办法", "应急处置操作手册", "制度阅读确认", "制度到期复审"],
            categories: ["管理制度", "操作手册", "应急预案", "监管文件", "培训材料"],
            owners: ["制度台账专员", "制度发布审批人", "制度复审专员", "文档管理员", "风险管理负责人"],
            versions: ["V1.0", "V2.0", "V2.1", "年度复审", "临时修订"]
        },
        RISK_COMPLIANCE: {
            titles: ["运维风险登记", "合规检查计划", "问题整改任务", "监管要求对照", "整改复核关闭"],
            categories: ["业务连续性", "信息安全", "数据安全", "监管合规", "操作风险"],
            owners: ["风险台账专员", "整改执行专员", "整改评审专员", "风险复核专员", "合规负责人"],
            versions: ["整改批次1", "整改批次2", "月度检查", "季度检查", "监管专项"]
        }
    };

    var standardBusinessDescriptions = [
        "资料完整，可按标准流程办理",
        "已完成前置检查，等待下一岗位处理",
        "涉及生产环境，需重点核对风险与回退措施",
        "需在计划日期前完成并保留过程记录"
    ];
    var standardReviewOpinions = [
        "资料完整，评审通过，建议进入下一环节",
        "基本符合要求，补充过程记录后继续办理",
        "存在一般风险，需在执行阶段重点跟踪",
        "不符合当前要求，建议驳回并重新提交"
    ];
    var standardRejectReasons = [
        "申请资料不完整，请补充后重新提交",
        "风险分析不充分，请完善影响评估",
        "回退或应急方案不完整",
        "不符合当前制度或技术规范",
        "审批依据不足，暂不通过"
    ];

    document.getElementById("loginForm").addEventListener("submit", handleLogin);
    document.getElementById("logoutButton").addEventListener("click", logout);
    document.getElementById("modalClose").addEventListener("click", closeModal);
    modalBackdrop.addEventListener("click", function (event) {
        if (event.target === modalBackdrop) closeModal();
    });
    document.getElementById("menuToggle").addEventListener("click", function () {
        document.querySelector(".sidebar").classList.toggle("open");
    });
    document.querySelectorAll(".nav-item").forEach(function (button) {
        button.addEventListener("click", function () {
            navigate(button.dataset.view);
            document.querySelector(".sidebar").classList.remove("open");
        });
    });

    setInterval(updateClock, 1000);
    updateClock();
    restoreSession();

    async function restoreSession() {
        if (!state.token) {
            showLogin();
            return;
        }
        try {
            state.user = await api("/api/auth/me");
            showApp();
            navigate("overview");
        } catch (error) {
            clearSession();
            showLogin();
        }
    }

    async function handleLogin(event) {
        event.preventDefault();
        var button = document.getElementById("loginButton");
        var errorBox = document.getElementById("loginError");
        var username = document.getElementById("loginUsername").value.trim();
        var password = document.getElementById("loginPassword").value;
        errorBox.textContent = "";
        button.disabled = true;
        button.textContent = "正在验证...";
        try {
            var result = await api("/api/auth/login", {
                method: "POST",
                body: JSON.stringify({ username: username, password: password }),
                anonymous: true
            });
            state.token = result.token;
            state.user = result.user;
            localStorage.setItem("bankops_token", state.token);
            showApp();
            navigate("overview");
            showToast("登录成功，欢迎 " + state.user.displayName);
        } catch (error) {
            errorBox.textContent = error.message;
        } finally {
            button.disabled = false;
            button.textContent = "安全登录";
        }
    }

    async function logout() {
        try {
            await api("/api/auth/logout", { method: "POST" });
        } catch (ignore) {
        }
        clearSession();
        showLogin();
        showToast("已安全退出");
    }

    function clearSession() {
        state.token = "";
        state.user = null;
        localStorage.removeItem("bankops_token");
    }

    function showLogin() {
        loginView.classList.remove("hidden");
        appView.classList.add("hidden");
        document.getElementById("loginPassword").value = "";
        document.getElementById("loginError").textContent = "";
    }

    function showApp() {
        loginView.classList.add("hidden");
        appView.classList.remove("hidden");
        var isAdmin = state.user.role === "ADMIN";
        document.querySelectorAll(".admin-only").forEach(function (element) {
            element.classList.toggle("hidden", !isAdmin);
        });
        document.querySelectorAll(".member-only").forEach(function (element) {
            element.classList.toggle("hidden", isAdmin);
        });
        var hasSpecialty = Boolean(state.user.specialtyModule && state.user.specialistDuty);
        document.querySelectorAll(".governance-section").forEach(function (element) {
            element.classList.toggle("hidden", !isAdmin && !hasSpecialty);
        });
        document.querySelectorAll(".governance-nav").forEach(function (element) {
            element.classList.toggle("hidden", !isAdmin && element.dataset.module !== state.user.specialtyModule);
        });
        document.getElementById("userDisplayName").textContent = state.user.displayName;
        document.getElementById("userRole").textContent = isAdmin ? "平台管理员" :
            (hasSpecialty ? moduleLabel(state.user.specialtyModule) + " · " + dutyLabel(state.user.specialistDuty) : "普通成员");
        document.getElementById("userAvatar").textContent = state.user.displayName.charAt(0);
    }

    async function navigate(view) {
        if ((view === "orders" || view === "users" || view === "audits") && state.user.role !== "ADMIN") {
            view = "overview";
        }
        if (governanceConfigs[view] && !canAccessGovernance(view)) view = "overview";
        if (view === "tasks" && state.user.role !== "MEMBER") view = "overview";
        state.currentView = view;
        document.querySelectorAll(".nav-item").forEach(function (button) {
            button.classList.toggle("active", button.dataset.view === view);
        });
        document.getElementById("pageTitle").textContent = pageMeta[view][0];
        document.getElementById("pageEyebrow").textContent = pageMeta[view][1];
        content.innerHTML = '<div class="loading"><div class="spinner"></div></div>';
        try {
            if (view === "overview") await renderOverview();
            if (view === "tasks") await renderTasks();
            if (view === "orders") await renderOrders();
            if (view === "assets") await renderAssets();
            if (governanceConfigs[view]) await renderGovernance(view);
            if (view === "users") await renderUsers();
            if (view === "audits") await renderAudits();
        } catch (error) {
            content.innerHTML = '<div class="panel empty-state"><b>页面加载失败</b>' + escapeHtml(error.message) + '</div>';
        }
    }

    async function renderOverview() {
        var data = await api("/api/dashboard");
        var isAdmin = state.user.role === "ADMIN";
        var assetTotal = data.assetStatus.online + data.assetStatus.maintenance + data.assetStatus.offline;
        var onlineRate = assetTotal ? Math.round(data.assetStatus.online / assetTotal * 100) : 0;
        var recentRows = data.recentOrders.map(orderRow).join("");
        content.innerHTML =
            '<section class="hero-banner">' +
                '<div class="hero-copy"><span class="eyebrow">BANK OPERATIONS COMMAND</span>' +
                '<h3>' + timeGreeting() + '，' + escapeHtml(state.user.displayName) + '</h3></div>' +
                '<div class="hero-state"><span class="pulse-dot"></span><div><strong>核心服务可用</strong></div></div>' +
            '</section>' +
            '<section class="metric-grid">' +
                metricCard(isAdmin ? "待处理工单" : "我的待处理", isAdmin ? data.pendingOrders : data.orderStatus.pending, "▤", "#e8f8f6", "#078b89") +
                (isAdmin ? '' : metricCard("我的处理中", data.orderStatus.processing, "↻", "#eaf0ff", "#3269cf")) +
                metricCard("在线资产", data.onlineAssets, "◇", "#eaf0ff", "#3269cf") +
                metricCard("维护中资产", data.maintenanceAssets, "⚙", "#fff1e6", "#b76b2f") +
                (isAdmin ? metricCard("异常登录 / 24h", data.failedLogins24h, "!", "#fdeaea", "#bd4545") : '') +
            '</section>' +
            '<section class="dashboard-grid">' +
                '<div class="panel"><div class="panel-header"><h3>资产运行状态</h3></div>' +
                    '<div class="panel-body status-overview">' +
                        '<div class="donut" style="--online:' + onlineRate + '%"><div class="donut-label"><strong>' + onlineRate + '%</strong><span>在线率</span></div></div>' +
                        '<div class="legend-list">' +
                            legendRow("#10b8b2", "在线资产", data.assetStatus.online) +
                            legendRow("#ff9a5a", "维护中", data.assetStatus.maintenance) +
                            legendRow("#d9e2e8", "离线资产", data.assetStatus.offline) +
                        '</div>' +
                    '</div>' +
                '</div>' +
                '<div class="panel"><div class="panel-header"><h3>快捷工作台</h3></div>' +
                    '<div class="panel-body quick-list">' +
                        (isAdmin
                            ? quickItem("projects", "▣", "查看项目看板") + quickItem("changes", "⇄", "审批变更发布") + quickItem("risks", "△", "跟踪风险整改")
                            : (state.user.specialtyModule ? quickItem(viewForModule(state.user.specialtyModule), "▦", "进入我的专业模块") : '') +
                                quickItem("tasks", "✓", "处理我的待办") + quickItem("assets", "◇", "查看资产台账")) +
                    '</div>' +
                '</div>' +
            '</section>' +
            '<section class="panel table-panel" style="margin-top:18px">' +
                '<div class="panel-header"><h3>' + (isAdmin ? '最近工单' : '我的最近任务') + '</h3><button class="link-button" data-route="' + (isAdmin ? 'orders' : 'tasks') + '">查看全部</button></div>' +
                '<div class="table-wrap"><table class="data-table"><thead><tr><th>工单</th><th>业务系统</th><th>类型</th><th>优先级</th><th>状态</th><th>负责人</th></tr></thead>' +
                '<tbody>' + (recentRows || emptyRow(6)) + '</tbody></table></div>' +
            '</section>';
        content.querySelectorAll("[data-route]").forEach(function (element) {
            element.addEventListener("click", function () { navigate(element.dataset.route); });
        });
    }

    function metricCard(label, value, icon, bg, color) {
        return '<div class="metric-card" style="--metric-bg:' + bg + ';--metric-color:' + color + '">' +
            '<div class="metric-top"><span>' + label + '</span><span class="metric-icon">' + icon + '</span></div>' +
            '<strong>' + value + '</strong></div>';
    }

    function legendRow(color, label, value) {
        return '<div class="legend-row"><i class="legend-dot" style="background:' + color + '"></i><span>' + label + '</span><strong>' + value + '</strong></div>';
    }

    function quickItem(route, icon, title) {
        return '<button class="quick-item" data-route="' + route + '"><span class="quick-item-icon">' + icon + '</span>' +
            '<span><strong>' + title + '</strong></span><b>›</b></button>';
    }

    async function renderTasks(filter) {
        state.taskFilter = filter || state.taskFilter || "PENDING";
        state.orders = await api("/api/work-orders/my");
        var counts = {
            PENDING: state.orders.filter(function (item) { return item.status === "PENDING"; }).length,
            PROCESSING: state.orders.filter(function (item) { return item.status === "PROCESSING"; }).length,
            RESOLVED: state.orders.filter(function (item) { return item.status === "RESOLVED"; }).length
        };
        var visible = state.orders.filter(function (item) { return item.status === state.taskFilter; });
        content.innerHTML =
            pageActions("待办工作", '<button id="refreshTasks" class="ghost-button">刷新</button>') +
            '<section class="task-summary">' +
                taskSummaryCard("待领取", counts.PENDING, "#e8f8f6", "#078b89") +
                taskSummaryCard("处理中", counts.PROCESSING, "#eaf0ff", "#3269cf") +
                taskSummaryCard("已完成", counts.RESOLVED, "#e8f7ef", "#27855b") +
            '</section>' +
            '<section class="panel table-panel">' +
                '<div class="task-tabs">' +
                    taskTab("PENDING", "待办工作", counts.PENDING) +
                    taskTab("PROCESSING", "处理中", counts.PROCESSING) +
                    taskTab("RESOLVED", "已办工作", counts.RESOLVED) +
                '</div>' +
                '<div class="table-wrap"><table class="data-table"><thead><tr><th>工单信息</th><th>业务系统</th><th>类型</th><th>优先级</th><th>状态</th><th>负责人</th><th>更新时间</th><th>操作</th></tr></thead>' +
                '<tbody>' + (visible.map(taskRow).join("") || '<tr><td colspan="8"><div class="empty-state"><b>当前没有' + statusLabel(state.taskFilter) + '任务</b></div></td></tr>') + '</tbody></table></div>' +
            '</section>';
        document.getElementById("refreshTasks").addEventListener("click", function () { renderTasks(state.taskFilter); });
        content.querySelectorAll("[data-task-filter]").forEach(function (button) {
            button.addEventListener("click", function () { renderTasks(button.dataset.taskFilter); });
        });
        content.querySelectorAll("[data-start-task]").forEach(function (button) {
            button.addEventListener("click", function () { startTask(Number(button.dataset.startTask)); });
        });
        content.querySelectorAll("[data-finish-task]").forEach(function (button) {
            button.addEventListener("click", function () { finishTask(Number(button.dataset.finishTask)); });
        });
    }

    function taskSummaryCard(label, value, bg, color) {
        return '<div class="task-summary-card" style="--task-bg:' + bg + ';--task-color:' + color + '"><span>' + label + '</span><strong>' + value + '</strong></div>';
    }

    function taskTab(status, label, count) {
        return '<button class="task-tab ' + (state.taskFilter === status ? 'active' : '') + '" data-task-filter="' + status + '">' + label + '<span>' + count + '</span></button>';
    }

    function taskRow(item) {
        var action = '—';
        if (item.status === "PENDING") action = '<button class="link-button" data-start-task="' + item.id + '">开始处理</button>';
        if (item.status === "PROCESSING") action = '<button class="link-button" data-finish-task="' + item.id + '">完成任务</button>';
        return '<tr><td><div class="table-title">' + escapeHtml(item.title) + '</div><div class="table-sub">' + escapeHtml(item.ticketNo) + '</div></td>' +
            '<td>' + escapeHtml(item.systemName) + '</td><td>' + escapeHtml(item.category) + '</td><td>' + priorityBadge(item.priority) + '</td>' +
            '<td>' + statusBadge(item.status) + '</td><td>' + escapeHtml(item.assignee || "待领取") + '</td><td>' + formatDate(item.updatedAt) + '</td>' +
            '<td><div class="table-actions">' + action + '</div></td></tr>';
    }

    async function startTask(id) {
        if (!confirm("确认领取并开始处理该任务？")) return;
        try {
            await api("/api/work-orders/" + id + "/start", { method: "PUT" });
            state.taskFilter = "PROCESSING";
            showToast("任务已领取，可以开始处理");
            renderTasks();
        } catch (error) {
            showToast(error.message, true);
        }
    }

    async function finishTask(id) {
        if (!confirm("确认该任务已经处理完成？")) return;
        try {
            await api("/api/work-orders/" + id + "/complete", { method: "PUT" });
            state.taskFilter = "RESOLVED";
            showToast("任务已完成");
            renderTasks();
        } catch (error) {
            showToast(error.message, true);
        }
    }

    async function renderOrders() {
        state.orders = await api("/api/work-orders");
        var isAdmin = state.user.role === "ADMIN";
        content.innerHTML =
            pageActions("运维工单", isAdmin ? '<button id="addOrder" class="primary-button">＋ 新建工单</button>' : '') +
            '<section class="panel table-panel"><div class="table-wrap"><table class="data-table">' +
            '<thead><tr><th>工单信息</th><th>业务系统</th><th>类型</th><th>优先级</th><th>状态</th><th>负责人</th><th>更新时间</th><th>操作</th></tr></thead>' +
            '<tbody>' + (state.orders.map(function (item) {
                return '<tr><td><div class="table-title">' + escapeHtml(item.title) + '</div><div class="table-sub">' + escapeHtml(item.ticketNo) + '</div></td>' +
                    '<td>' + escapeHtml(item.systemName) + '</td><td>' + escapeHtml(item.category) + '</td>' +
                    '<td>' + priorityBadge(item.priority) + '</td><td>' + statusBadge(item.status) + '</td>' +
                    '<td>' + escapeHtml(item.assignee || "未分配") + '</td><td>' + formatDate(item.updatedAt) + '</td>' +
                    '<td><div class="table-actions">' +
                    '<button class="link-button" data-edit-order="' + item.id + '">编辑</button><button class="link-button danger" data-delete-order="' + item.id + '">删除</button>' +
                    '</div></td></tr>';
            }).join("") || emptyRow(8)) + '</tbody></table></div></section>';
        if (isAdmin) document.getElementById("addOrder").addEventListener("click", function () { openOrderModal(null); });
        content.querySelectorAll("[data-edit-order]").forEach(function (button) {
            button.addEventListener("click", function () {
                openOrderModal(state.orders.find(function (item) { return item.id === Number(button.dataset.editOrder); }));
            });
        });
        content.querySelectorAll("[data-delete-order]").forEach(function (button) {
            button.addEventListener("click", function () { deleteOrder(Number(button.dataset.deleteOrder)); });
        });
    }

    function openOrderModal(item) {
        var editing = Boolean(item);
        openModal(editing ? "编辑运维工单" : "新建运维工单",
            '<div class="form-grid">' +
                inputField("title", "工单标题", item && item.title, "例如：核心网关延迟排查", true, "full") +
                selectField("category", "工单类型", ["故障事件", "变更任务", "服务请求", "安全事件"], item && item.category) +
                selectField("priority", "优先级", ["高", "中", "低"], item && item.priority) +
                inputField("systemName", "业务系统", item && item.systemName, "例如：统一支付平台", true) +
                inputField("assignee", "负责人", item && item.assignee, "例如：普通成员", false) +
                selectField("status", "处理状态", ["PENDING", "PROCESSING", "RESOLVED", "CLOSED"], item && item.status, statusLabel) +
                '<label class="field full"><span>处理说明</span><textarea name="description" maxlength="1000" placeholder="记录现象、处理动作与验证结果">' + escapeHtml(item && item.description || "") + '</textarea></label>' +
                modalActions(editing ? "保存修改" : "创建工单") +
            '</div>',
            async function (form) {
                var body = formObject(form);
                await api("/api/work-orders" + (editing ? "/" + item.id : ""), {
                    method: editing ? "PUT" : "POST",
                    body: JSON.stringify(body)
                });
                closeModal();
                showToast(editing ? "工单已更新" : "工单已创建");
                renderOrders();
            });
    }

    async function deleteOrder(id) {
        if (!confirm("确认删除这条工单？此操作不可撤销。")) return;
        try {
            await api("/api/work-orders/" + id, { method: "DELETE" });
            showToast("工单已删除");
            renderOrders();
        } catch (error) {
            showToast(error.message, true);
        }
    }

    async function renderAssets() {
        state.assets = await api("/api/assets");
        var isAdmin = state.user.role === "ADMIN";
        content.innerHTML =
            pageActions("资产台账", isAdmin ? '<button id="addAsset" class="primary-button">＋ 新增资产</button>' : '') +
            '<section class="panel table-panel"><div class="table-wrap"><table class="data-table">' +
            '<thead><tr><th>资产信息</th><th>类型</th><th>IP 地址</th><th>环境</th><th>状态</th><th>责任人</th><th>更新时间</th><th>操作</th></tr></thead>' +
            '<tbody>' + (state.assets.map(function (item) {
                return '<tr><td><div class="table-title">' + escapeHtml(item.assetName) + '</div><div class="table-sub">' + escapeHtml(item.assetCode) + '</div></td>' +
                    '<td>' + escapeHtml(item.assetType) + '</td><td><code>' + escapeHtml(item.ipAddress) + '</code></td><td>' + escapeHtml(item.environment) + '</td>' +
                    '<td>' + assetStatusBadge(item.status) + '</td><td>' + escapeHtml(item.owner) + '</td><td>' + formatDate(item.updatedAt) + '</td>' +
                    '<td><div class="table-actions">' +
                    (isAdmin ? '<button class="link-button" data-edit-asset="' + item.id + '">编辑</button><button class="link-button danger" data-delete-asset="' + item.id + '">删除</button>' : '—') +
                    '</div></td></tr>';
            }).join("") || emptyRow(8)) + '</tbody></table></div></section>';
        if (isAdmin) document.getElementById("addAsset").addEventListener("click", function () { openAssetModal(null); });
        content.querySelectorAll("[data-edit-asset]").forEach(function (button) {
            button.addEventListener("click", function () {
                openAssetModal(state.assets.find(function (item) { return item.id === Number(button.dataset.editAsset); }));
            });
        });
        content.querySelectorAll("[data-delete-asset]").forEach(function (button) {
            button.addEventListener("click", function () { deleteAsset(Number(button.dataset.deleteAsset)); });
        });
    }

    function openAssetModal(item) {
        var editing = Boolean(item);
        openModal(editing ? "编辑资产" : "新增资产",
            '<div class="form-grid">' +
                inputField("assetCode", "资产编号", item && item.assetCode, "例如：APP-PAY-02", true) +
                inputField("assetName", "资产名称", item && item.assetName, "例如：支付应用节点02", true) +
                selectField("assetType", "资产类型", ["应用服务器", "数据库", "安全网关", "网络设备", "监控节点"], item && item.assetType) +
                inputField("ipAddress", "IP 地址", item && item.ipAddress, "例如：10.20.1.12", true) +
                selectField("environment", "运行环境", ["生产", "灾备", "测试", "开发"], item && item.environment) +
                selectField("status", "资产状态", ["ONLINE", "MAINTENANCE", "OFFLINE"], item && item.status, assetStatusLabel) +
                inputField("owner", "责任人/组", item && item.owner, "例如：平台管理员", true, "full") +
                '<label class="field full"><span>资产说明</span><textarea name="description" maxlength="500">' + escapeHtml(item && item.description || "") + '</textarea></label>' +
                modalActions(editing ? "保存修改" : "新增资产") +
            '</div>',
            async function (form) {
                await api("/api/assets" + (editing ? "/" + item.id : ""), {
                    method: editing ? "PUT" : "POST",
                    body: JSON.stringify(formObject(form))
                });
                closeModal();
                showToast(editing ? "资产信息已更新" : "资产已新增");
                renderAssets();
            });
    }

    async function deleteAsset(id) {
        if (!confirm("确认删除这条资产记录？")) return;
        try {
            await api("/api/assets/" + id, { method: "DELETE" });
            showToast("资产已删除");
            renderAssets();
        } catch (error) {
            showToast(error.message, true);
        }
    }

    async function renderGovernance(view, filters) {
        var config = governanceConfigs[view];
        filters = filters || state.governanceFilters[view] || {};
        state.governanceFilters[view] = filters;
        var loaded = await Promise.all([
            api("/api/governance/" + config.module + "?" + governanceQuery(filters)),
            api("/api/governance/" + config.module + "/workflow")
        ]);
        state.governanceRecords = loaded[0];
        state.governanceWorkflow = loaded[1];
        var records = state.governanceRecords;
        var waiting = records.filter(function (item) { return /待|审批|评审|会签|复核/.test(item.status); }).length;
        var highRisk = records.filter(function (item) { return item.riskLevel === "高"; }).length;
        var nonCompliant = records.filter(function (item) { return item.compliant === false; }).length;
        content.innerHTML =
            pageActions(config.title,
                '<button id="exportGovernance" class="ghost-button">导出报表</button>' +
                (canGovernanceDuty(config.module, "REGISTER") ? '<button id="addGovernance" class="primary-button">＋ 新增记录</button>' : '')) +
            '<section class="feature-grid">' + config.features.map(function (feature, index) {
                return '<div class="feature-card"><span>' + String(index + 1).padStart(2, "0") + '</span><strong>' + escapeHtml(feature) + '</strong></div>';
            }).join("") + '</section>' +
            '<section class="task-summary governance-summary">' +
                taskSummaryCard("当前记录", records.length, "#e8f8f6", "#078b89") +
                taskSummaryCard("待审批 / 评审", waiting, "#eaf0ff", "#3269cf") +
                taskSummaryCard("高风险 / 不合规", highRisk + nonCompliant, "#fdeaea", "#bd4545") +
            '</section>' +
            '<section class="panel governance-filter-panel"><form id="governanceFilter" class="governance-filters">' +
                '<label><span>关键字</span><input name="keyword" value="' + escapeHtml(filters.keyword || "") + '" placeholder="名称、编号、负责人"></label>' +
                governanceFilterSelect("department", "部门", config.departments, filters.department) +
                governanceFilterSelect("status", "状态", config.statuses, filters.status) +
                governanceFilterSelect("category", config.categoryLabel,
                    uniqueOptions((governanceFormOptions[config.module] || {}).categories || [], records.map(function (item) { return item.category; })),
                    filters.category) +
                '<label><span>开始日期</span><input type="date" name="fromDate" value="' + escapeHtml(filters.fromDate || "") + '"></label>' +
                '<label><span>结束日期</span><input type="date" name="toDate" value="' + escapeHtml(filters.toDate || "") + '"></label>' +
                '<div class="filter-actions"><button class="primary-button" type="submit">筛选</button><button id="resetGovernanceFilter" class="ghost-button" type="button">重置</button></div>' +
            '</form></section>' +
            '<section class="panel table-panel"><div class="table-wrap"><table class="data-table governance-table">' +
                '<thead><tr><th>编号 / 名称</th><th>业务类型</th><th>部门</th><th>' + escapeHtml(config.categoryLabel) + '</th><th>状态</th><th>负责人</th><th>风险 / 合规</th><th>' + escapeHtml(config.dateLabel) + '</th><th>操作</th></tr></thead>' +
                '<tbody>' + (records.map(function (item) { return governanceRow(view, item); }).join("") || '<tr><td colspan="9"><div class="empty-state"><b>没有符合条件的记录</b></div></td></tr>') + '</tbody>' +
            '</table></div></section>';

        if (document.getElementById("addGovernance")) {
            document.getElementById("addGovernance").addEventListener("click", function () { openGovernanceModal(view, null); });
        }
        document.getElementById("exportGovernance").addEventListener("click", function () { downloadGovernance(view); });
        document.getElementById("governanceFilter").addEventListener("submit", function (event) {
            event.preventDefault();
            renderGovernance(view, formObject(event.currentTarget));
        });
        document.getElementById("resetGovernanceFilter").addEventListener("click", function () { renderGovernance(view, {}); });
        content.querySelectorAll("[data-edit-governance]").forEach(function (button) {
            button.addEventListener("click", function () {
                openGovernanceModal(view, records.find(function (item) { return item.id === Number(button.dataset.editGovernance); }));
            });
        });
        content.querySelectorAll("[data-delete-governance]").forEach(function (button) {
            button.addEventListener("click", function () { deleteGovernance(view, Number(button.dataset.deleteGovernance)); });
        });
        content.querySelectorAll("[data-next-governance]").forEach(function (button) {
            button.addEventListener("click", function () { advanceGovernance(view, Number(button.dataset.nextGovernance)); });
        });
        content.querySelectorAll("[data-reject-governance]").forEach(function (button) {
            button.addEventListener("click", function () { openGovernanceRejectModal(view, Number(button.dataset.rejectGovernance)); });
        });
        content.querySelectorAll("[data-review-governance]").forEach(function (button) {
            button.addEventListener("click", function () {
                openGovernanceReviewModal(view, records.find(function (item) { return item.id === Number(button.dataset.reviewGovernance); }));
            });
        });
    }

    function governanceRow(view, item) {
        var config = governanceConfigs[view];
        var step = state.governanceWorkflow[item.status];
        var followingStep = step && state.governanceWorkflow[step.nextStatus];
        var handoff = step
            ? '<div class="table-sub">当前：' + escapeHtml(dutyLabel(step.duty)) + (followingStep ? ' → 下一步：' + escapeHtml(dutyLabel(followingStep.duty)) : ' → 完成流程') + '</div>'
            : '<div class="table-sub">流程已完成</div>';
        var actions = '';
        if (step && canGovernanceDuty(config.module, step.duty)) {
            actions += '<button class="link-button" data-next-governance="' + item.id + '">' + escapeHtml(step.actionLabel) + '</button>';
            if ((step.duty === "REVIEW" || step.duty === "APPROVE") && config.rejectStatus) {
                actions += '<button class="link-button danger" data-reject-governance="' + item.id + '">驳回</button>';
            }
        }
        if (canGovernanceDuty(config.module, "REVIEW")) {
            actions += '<button class="link-button" data-review-governance="' + item.id + '">填写评审</button>';
        }
        if (canGovernanceDuty(config.module, "REGISTER")) {
            actions += '<button class="link-button" data-edit-governance="' + item.id + '">编辑</button>' +
                '<button class="link-button danger" data-delete-governance="' + item.id + '">删除</button>';
        }
        if (!actions) actions = '—';
        return '<tr><td><div class="table-title">' + escapeHtml(item.title) + '</div><div class="table-sub">' + escapeHtml(item.recordNo) + (item.versionNo ? ' · ' + escapeHtml(item.versionNo) : '') + '</div></td>' +
            '<td>' + escapeHtml(item.recordType) + '</td><td>' + escapeHtml(item.department) + '</td><td>' + escapeHtml(item.category || "-") + '</td>' +
            '<td>' + governanceStatusBadge(item.status) + handoff + '</td><td>' + escapeHtml(item.owner) + '</td>' +
            '<td><div class="risk-stack">' + riskBadge(item.riskLevel) + complianceBadge(item.compliant) + '</div></td>' +
            '<td>' + escapeHtml(item.plannedDate || "-") + '</td><td><div class="table-actions governance-actions">' + actions + '</div></td></tr>';
    }

    function openGovernanceModal(view, item) {
        var config = governanceConfigs[view];
        var formOptions = governanceFormOptions[config.module];
        var editing = Boolean(item);
        var statusOptions = optionsWithCurrent(config.statuses, item && item.status);
        var typeOptions = optionsWithCurrent(config.types, item && item.recordType);
        var statusValue = item && item.status || config.statuses[0];
        var scoreValue = item && item.score != null ? String(item.score) : "";
        var statusField = state.user.role === "ADMIN"
            ? selectField("status", "当前状态", statusOptions, statusValue)
            : '<input type="hidden" name="status" value="' + escapeHtml(statusValue) + '">';
        openModal(editing ? "编辑" + config.title + "记录" : "新增" + config.title + "记录",
            '<div class="form-grid">' +
                selectField("title", "名称 / 事项", optionsWithCurrent(formOptions.titles, item && item.title), item && item.title, null, "full") +
                selectField("recordType", "业务类型", typeOptions, item && item.recordType) +
                selectField("department", "所属部门", optionsWithCurrent(config.departments, item && item.department), item && item.department) +
                selectField("category", config.categoryLabel, optionsWithCurrent(formOptions.categories, item && item.category), item && item.category) +
                selectField("owner", "负责人", optionsWithCurrent(formOptions.owners, item && item.owner), item && item.owner) +
                statusField +
                '<label class="field"><span>' + escapeHtml(config.dateLabel) + '</span><input type="date" name="plannedDate" value="' + escapeHtml(item && item.plannedDate || "") + '"></label>' +
                selectField("priority", "优先级", optionsWithCurrent(["高", "中", "低"], item && item.priority), item && item.priority || "中") +
                selectField("riskLevel", "风险等级", optionsWithCurrent(["高", "中", "低"], item && item.riskLevel), item && item.riskLevel || "低") +
                selectField("versionNo", "版本 / 周期", optionsWithCurrent(formOptions.versions, item && item.versionNo), item && item.versionNo) +
                selectField("score", "评分 / 进度", optionsWithCurrent(["", "0", "20", "40", "60", "80", "100"], scoreValue), scoreValue, scoreOptionLabel) +
                selectField("compliant", "合规检查", ["true", "false"], item ? String(item.compliant) : "true", function (value) { return value === "true" ? "符合" : "不符合"; }) +
                selectField("details", "评审意见 / 业务说明", optionsWithCurrent(standardBusinessDescriptions, item && item.details), item && item.details, null, "full") +
                modalActions(editing ? "保存修改" : "新增记录") +
            '</div>',
            async function (form) {
                var body = formObject(form);
                body.plannedDate = body.plannedDate || null;
                body.score = body.score ? Number(body.score) : null;
                body.compliant = body.compliant === "true";
                await api("/api/governance/" + config.module + (editing ? "/" + item.id : ""), {
                    method: editing ? "PUT" : "POST",
                    body: JSON.stringify(body)
                });
                closeModal();
                showToast(editing ? "业务记录已更新" : "业务记录已新增");
                renderGovernance(view);
            });
    }

    async function advanceGovernance(view, id) {
        var record = state.governanceRecords.find(function (item) { return item.id === id; });
        var step = record && state.governanceWorkflow[record.status];
        var followingStep = step && state.governanceWorkflow[step.nextStatus];
        var handoffText = followingStep ? "，完成后交给“" + dutyLabel(followingStep.duty) + "”岗位" : "，完成后流程结束";
        if (!step || !confirm("确认执行“" + step.actionLabel + "”，将状态更新为“" + step.nextStatus + "”" + handoffText + "？")) return;
        try {
            await api("/api/governance/" + governanceConfigs[view].module + "/" + id + "/advance", { method: "PUT" });
            showToast("流程已推进至“" + step.nextStatus + "”" + handoffText);
            renderGovernance(view);
        } catch (error) {
            showToast(error.message, true);
        }
    }

    function openGovernanceReviewModal(view, item) {
        var scoreValue = item.score != null ? String(item.score) : "";
        openModal("填写评审意见",
            '<div class="form-grid">' +
                selectField("score", "评审评分", optionsWithCurrent(["", "0", "20", "40", "60", "80", "100"], scoreValue), scoreValue, scoreOptionLabel) +
                selectField("compliant", "合规结论", ["true", "false"], String(item.compliant), function (value) { return value === "true" ? "符合" : "不符合"; }) +
                selectField("details", "评审意见", optionsWithCurrent(standardReviewOpinions, item.details), item.details, null, "full") +
                modalActions("保存评审") + '</div>',
            async function (form) {
                var body = formObject(form);
                body.score = body.score ? Number(body.score) : null;
                body.compliant = body.compliant === "true";
                await api("/api/governance/" + governanceConfigs[view].module + "/" + item.id + "/review", {
                    method: "PUT", body: JSON.stringify(body)
                });
                closeModal();
                showToast("评审意见已保存");
                renderGovernance(view);
            });
    }

    function openGovernanceRejectModal(view, id) {
        openModal("填写驳回原因",
            '<div class="form-grid">' + selectField("reason", "驳回原因", standardRejectReasons, standardRejectReasons[0], null, "full") +
                modalActions("确认驳回") + '</div>',
            async function (form) {
                await api("/api/governance/" + governanceConfigs[view].module + "/" + id + "/reject", {
                    method: "PUT", body: JSON.stringify(formObject(form))
                });
                closeModal();
                showToast("记录已驳回");
                renderGovernance(view);
            });
    }

    async function deleteGovernance(view, id) {
        if (!confirm("确认删除这条管理记录？")) return;
        try {
            await api("/api/governance/" + governanceConfigs[view].module + "/" + id, { method: "DELETE" });
            showToast("管理记录已删除");
            renderGovernance(view);
        } catch (error) {
            showToast(error.message, true);
        }
    }

    async function downloadGovernance(view) {
        var config = governanceConfigs[view];
        try {
            var response = await fetch(API_BASE + "/api/governance/" + config.module + "/export?" + governanceQuery(state.governanceFilters[view] || {}), {
                headers: { "X-Auth-Token": state.token }
            });
            if (!response.ok) throw new Error("报表导出失败");
            var url = URL.createObjectURL(await response.blob());
            var link = document.createElement("a");
            link.href = url;
            link.download = config.title + "-" + new Date().toISOString().slice(0, 10) + ".csv";
            document.body.appendChild(link);
            link.click();
            link.remove();
            URL.revokeObjectURL(url);
            showToast("报表已导出");
        } catch (error) {
            showToast(error.message, true);
        }
    }

    function governanceQuery(filters) {
        var params = new URLSearchParams();
        ["keyword", "department", "status", "category", "fromDate", "toDate"].forEach(function (name) {
            if (filters[name]) params.set(name, filters[name]);
        });
        return params.toString();
    }

    function governanceFilterSelect(name, label, options, selected) {
        return '<label><span>' + label + '</span><select name="' + name + '"><option value="">全部</option>' +
            options.map(function (option) {
                return '<option value="' + escapeHtml(option) + '"' + (option === selected ? ' selected' : '') + '>' + escapeHtml(option) + '</option>';
            }).join("") + '</select></label>';
    }

    function optionsWithCurrent(options, current) {
        var result = options.slice();
        if (current && result.indexOf(current) < 0) result.unshift(current);
        return result;
    }

    function uniqueOptions(base, additions) {
        var result = base.slice();
        additions.forEach(function (value) {
            if (value && result.indexOf(value) < 0) result.push(value);
        });
        return result;
    }

    function scoreOptionLabel(value) {
        return value === "" ? "暂不评分" : value + " 分 / %";
    }

    function governanceStatusBadge(value) {
        var className = /关闭|完成|通过|发布|批准|确认|服务中|运行中|使用中/.test(value) ? "badge-green" :
            (/驳回|回退|黑名单|下线|到期/.test(value) ? "badge-red" :
                (/待|审批|评审|会签|复核/.test(value) ? "badge-orange" : "badge-blue"));
        return '<span class="badge ' + className + '">' + escapeHtml(value) + '</span>';
    }

    function riskBadge(value) {
        var className = value === "高" ? "badge-red" : value === "中" ? "badge-orange" : "badge-blue";
        return '<span class="badge ' + className + '">' + escapeHtml(value || "未评级") + '</span>';
    }

    function complianceBadge(value) {
        return '<span class="badge ' + (value ? "badge-green" : "badge-red") + '">' + (value ? "合规" : "不合规") + '</span>';
    }

    async function renderUsers() {
        state.users = await api("/api/admin/users");
        content.innerHTML =
            pageActions("账号与权限", '<button id="addUser" class="primary-button">＋ 创建账号</button>') +
            '<section class="panel table-panel"><div class="table-wrap"><table class="data-table">' +
            '<thead><tr><th>账号</th><th>身份</th><th>负责模块</th><th>岗位职责</th><th>状态</th><th>失败次数</th><th>最后登录</th><th>创建时间</th><th>操作</th></tr></thead>' +
            '<tbody>' + state.users.map(function (item) {
                return '<tr><td><div class="table-title">' + escapeHtml(item.displayName) + '</div><div class="table-sub">' + escapeHtml(item.username) + '</div></td>' +
                    '<td>' + roleBadge(item.role, item.specialtyModule) + '</td><td>' + escapeHtml(moduleLabel(item.specialtyModule)) + '</td><td>' + escapeHtml(dutyLabel(item.specialistDuty)) + '</td>' +
                    '<td>' + accountStatusBadge(item.status) + '</td><td>' + item.failedLoginAttempts + ' / 5</td>' +
                    '<td>' + formatDate(item.lastLoginAt) + '</td><td>' + formatDate(item.createdAt) + '</td>' +
                    '<td><div class="table-actions">' + userStatusAction(item) +
                    (item.role === "MEMBER" ? '<button class="link-button" data-assign-user="' + item.id + '">调整分工</button>' : '') +
                    '<button class="link-button" data-reset-user="' + item.id + '">重置密码</button></div></td></tr>';
            }).join("") + '</tbody></table></div></section>';
        document.getElementById("addUser").addEventListener("click", openUserModal);
        content.querySelectorAll("[data-status-user]").forEach(function (button) {
            button.addEventListener("click", function () {
                changeUserStatus(Number(button.dataset.statusUser), button.dataset.status);
            });
        });
        content.querySelectorAll("[data-reset-user]").forEach(function (button) {
            button.addEventListener("click", function () { openResetPasswordModal(Number(button.dataset.resetUser)); });
        });
        content.querySelectorAll("[data-assign-user]").forEach(function (button) {
            button.addEventListener("click", function () { openAssignmentModal(Number(button.dataset.assignUser)); });
        });
    }

    function openUserModal() {
        openModal("管理员创建平台账号",
            '<div class="form-grid">' +
                inputField("username", "登录账号", "", "4-20位字母、数字、下划线", true) +
                inputField("displayName", "显示名称", "", "例如：张三", true) +
                selectField("role", "账号角色", ["MEMBER", "ADMIN"], "MEMBER", roleLabel) +
                '<label class="field"><span>初始密码</span><input type="password" name="password" minlength="8" maxlength="64" placeholder="字母+数字+特殊字符" required></label>' +
                selectField("specialtyModule", "负责模块", governanceModuleValues(), "", moduleLabel) +
                selectField("specialistDuty", "岗位职责", specialistDutyValues(), "", dutyLabel) +
                modalActions("创建账号") +
            '</div>',
            async function (form) {
                var body = formObject(form);
                body.specialtyModule = body.specialtyModule || null;
                body.specialistDuty = body.specialistDuty || null;
                await api("/api/admin/users", { method: "POST", body: JSON.stringify(body) });
                closeModal();
                showToast("新账号已创建");
                renderUsers();
            });
    }

    function openAssignmentModal(id) {
        var user = state.users.find(function (item) { return item.id === id; });
        openModal("调整专员分工",
            '<div class="form-grid"><div class="field full"><strong>' + escapeHtml(user.displayName) + '（' + escapeHtml(user.username) + '）</strong></div>' +
                selectField("specialtyModule", "负责模块", governanceModuleValues(), user.specialtyModule || "", moduleLabel) +
                selectField("specialistDuty", "岗位职责", specialistDutyValues(), user.specialistDuty || "", dutyLabel) +
                modalActions("保存分工") + '</div>',
            async function (form) {
                var body = formObject(form);
                body.specialtyModule = body.specialtyModule || null;
                body.specialistDuty = body.specialistDuty || null;
                await api("/api/admin/users/" + id + "/assignment", {
                    method: "PUT", body: JSON.stringify(body)
                });
                closeModal();
                showToast("专员分工已更新");
                renderUsers();
            });
    }

    function openResetPasswordModal(id) {
        var user = state.users.find(function (item) { return item.id === id; });
        openModal("重置账号密码",
            '<div class="form-grid"><div class="field full"><strong>' + escapeHtml(user.displayName) + '（' + escapeHtml(user.username) + '）</strong></div>' +
                '<label class="field full"><span>新密码</span><input type="password" name="newPassword" minlength="8" maxlength="64" placeholder="字母+数字+特殊字符" required></label>' +
                modalActions("确认重置") + '</div>',
            async function (form) {
                await api("/api/admin/users/" + id + "/password", {
                    method: "PUT", body: JSON.stringify(formObject(form))
                });
                closeModal();
                showToast("密码已重置，账号已解锁");
                renderUsers();
            });
    }

    async function changeUserStatus(id, status) {
        var labels = { ACTIVE: "启用/解锁", DISABLED: "停用", LOCKED: "锁定" };
        if (!confirm("确认" + labels[status] + "该账号？")) return;
        try {
            await api("/api/admin/users/" + id + "/status", {
                method: "PUT", body: JSON.stringify({ status: status })
            });
            showToast("账号状态已更新");
            renderUsers();
        } catch (error) {
            showToast(error.message, true);
        }
    }

    async function renderAudits() {
        var audits = await api("/api/admin/login-audits");
        content.innerHTML =
            pageActions("登录安全审计", '') +
            '<section class="panel table-panel"><div class="table-wrap"><table class="data-table">' +
            '<thead><tr><th>账号</th><th>结果</th><th>安全判定</th><th>来源 IP</th><th>时间</th></tr></thead>' +
            '<tbody>' + (audits.map(function (item) {
                return '<tr><td><div class="table-title">' + escapeHtml(item.username) + '</div></td>' +
                    '<td>' + (item.success ? '<span class="badge badge-green">成功</span>' : '<span class="badge badge-red">失败</span>') + '</td>' +
                    '<td>' + escapeHtml(item.reason) + '</td><td><code>' + escapeHtml(item.ipAddress || "-") + '</code></td><td>' + formatDate(item.loginTime) + '</td></tr>';
            }).join("") || emptyRow(5)) + '</tbody></table></div></section>';
    }

    function pageActions(title, actions) {
        return '<div class="page-actions"><div><h3>' + title + '</h3></div><div class="action-group">' + actions + '</div></div>';
    }

    function orderRow(item) {
        return '<tr><td><div class="table-title">' + escapeHtml(item.title) + '</div><div class="table-sub">' + escapeHtml(item.ticketNo) + '</div></td>' +
            '<td>' + escapeHtml(item.systemName) + '</td><td>' + escapeHtml(item.category) + '</td><td>' + priorityBadge(item.priority) + '</td>' +
            '<td>' + statusBadge(item.status) + '</td><td>' + escapeHtml(item.assignee || "未分配") + '</td></tr>';
    }

    function openModal(title, html, onSubmit) {
        document.getElementById("modalTitle").textContent = title;
        modalForm.innerHTML = html;
        modalBackdrop.classList.remove("hidden");
        modalForm.onsubmit = async function (event) {
            event.preventDefault();
            var submit = modalForm.querySelector('[type="submit"]');
            submit.disabled = true;
            var original = submit.textContent;
            submit.textContent = "处理中...";
            try {
                await onSubmit(modalForm);
            } catch (error) {
                showToast(error.message, true);
                submit.disabled = false;
                submit.textContent = original;
            }
        };
    }

    function closeModal() {
        modalBackdrop.classList.add("hidden");
        modalForm.innerHTML = "";
        modalForm.onsubmit = null;
    }

    function inputField(name, label, value, placeholder, required, className) {
        return '<label class="field ' + (className || "") + '"><span>' + label + '</span><input name="' + name + '" value="' +
            escapeHtml(value || "") + '" maxlength="100" placeholder="' + escapeHtml(placeholder || "") + '"' + (required ? " required" : "") + '></label>';
    }

    function selectField(name, label, options, selected, formatter, className) {
        return '<label class="field ' + (className || "") + '"><span>' + label + '</span><select name="' + name + '">' +
            options.map(function (option) {
                var text = formatter ? formatter(option) : option;
                return '<option value="' + escapeHtml(option) + '"' + (option === selected ? " selected" : "") + '>' + escapeHtml(text) + '</option>';
            }).join("") + '</select></label>';
    }

    function modalActions(label) {
        return '<div class="modal-actions"><button type="button" class="ghost-button" onclick="document.getElementById(\'modalClose\').click()">取消</button>' +
            '<button type="submit" class="primary-button">' + label + '</button></div>';
    }

    function formObject(form) {
        var result = {};
        new FormData(form).forEach(function (value, key) { result[key] = String(value).trim(); });
        return result;
    }

    async function api(path, options) {
        options = options || {};
        var headers = { "Content-Type": "application/json" };
        if (!options.anonymous && state.token) headers["X-Auth-Token"] = state.token;
        var response;
        try {
            response = await fetch(API_BASE + path, {
                method: options.method || "GET",
                headers: headers,
                body: options.body
            });
        } catch (error) {
            throw new Error("无法连接后端，请确认 IDEA 中的服务已启动（端口 8081）");
        }
        var text = await response.text();
        var data = text ? safeJson(text) : {};
        if (!response.ok) {
            if (response.status === 401 && !options.anonymous) {
                clearSession();
                showLogin();
            }
            throw new Error(data.message || ("请求失败：" + response.status));
        }
        return data;
    }

    function safeJson(text) {
        try { return JSON.parse(text); } catch (error) { return {}; }
    }

    function updateClock() {
        var now = new Date();
        document.getElementById("clockDate").textContent = now.toLocaleDateString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", weekday: "short" });
        document.getElementById("clockTime").textContent = now.toLocaleTimeString("zh-CN", { hour12: false });
    }

    function timeGreeting() {
        var hour = new Date().getHours();
        if (hour < 6) return "夜深了";
        if (hour < 12) return "早上好";
        if (hour < 18) return "下午好";
        return "晚上好";
    }

    function formatDate(value) {
        if (!value) return "-";
        return new Date(value).toLocaleString("zh-CN", { hour12: false }).replace(/\//g, "-");
    }

    function escapeHtml(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#039;");
    }

    function statusLabel(value) {
        return { PENDING: "待处理", PROCESSING: "处理中", RESOLVED: "已解决", CLOSED: "已关闭" }[value] || value;
    }

    function assetStatusLabel(value) {
        return { ONLINE: "在线", MAINTENANCE: "维护中", OFFLINE: "离线" }[value] || value;
    }

    function canAccessGovernance(view) {
        if (state.user.role === "ADMIN") return true;
        return governanceConfigs[view] && governanceConfigs[view].module === state.user.specialtyModule && Boolean(state.user.specialistDuty);
    }

    function canGovernanceDuty(module, duty) {
        return state.user.role === "ADMIN" || (state.user.specialtyModule === module && state.user.specialistDuty === duty);
    }

    function viewForModule(module) {
        var found = Object.keys(governanceConfigs).find(function (view) { return governanceConfigs[view].module === module; });
        return found || "overview";
    }

    function governanceModuleValues() {
        return ["", "ARCHITECTURE", "OUTSOURCING", "PROJECT", "REQUIREMENT", "CHANGE_RELEASE", "ASSET_RESOURCE", "PERSONNEL", "DOCUMENT", "RISK_COMPLIANCE"];
    }

    function specialistDutyValues() {
        return ["", "REGISTER", "REVIEW", "APPROVE", "OPERATE", "CLOSE"];
    }

    function moduleLabel(value) {
        return {
            ARCHITECTURE: "架构管理", OUTSOURCING: "外包管理", PROJECT: "项目管理", REQUIREMENT: "需求管理",
            CHANGE_RELEASE: "变更与发布", ASSET_RESOURCE: "资产与资源", PERSONNEL: "人员与团队",
            DOCUMENT: "制度与文档", RISK_COMPLIANCE: "风险与合规"
        }[value] || (value ? value : "无");
    }

    function dutyLabel(value) {
        return { REGISTER: "台账录入", REVIEW: "评审确认", APPROVE: "审批决策", OPERATE: "执行跟踪", CLOSE: "验收关闭" }[value] || (value ? value : "普通成员");
    }

    function roleLabel(value) {
        return { ADMIN: "管理员", MEMBER: "普通成员" }[value] || value;
    }

    function statusBadge(value) {
        var classes = { PENDING: "badge-orange", PROCESSING: "badge-blue", RESOLVED: "badge-green", CLOSED: "badge-gray" };
        return '<span class="badge ' + classes[value] + '">' + statusLabel(value) + '</span>';
    }

    function assetStatusBadge(value) {
        var classes = { ONLINE: "badge-green", MAINTENANCE: "badge-orange", OFFLINE: "badge-red" };
        return '<span class="badge ' + classes[value] + '">' + assetStatusLabel(value) + '</span>';
    }

    function priorityBadge(value) {
        var classes = { "高": "badge-red", "中": "badge-orange", "低": "badge-blue" };
        return '<span class="badge ' + (classes[value] || "badge-gray") + '">' + escapeHtml(value) + '</span>';
    }

    function roleBadge(value, specialtyModule) {
        var label = value === "ADMIN" ? "管理员" : (specialtyModule ? "模块专员" : "普通成员");
        return '<span class="badge ' + (value === "ADMIN" ? "badge-blue" : "badge-green") + '">' + label + '</span>';
    }

    function accountStatusBadge(value) {
        var labels = { ACTIVE: "正常", LOCKED: "已锁定", DISABLED: "已停用" };
        var classes = { ACTIVE: "badge-green", LOCKED: "badge-red", DISABLED: "badge-gray" };
        return '<span class="badge ' + classes[value] + '">' + labels[value] + '</span>';
    }

    function userStatusAction(item) {
        if (item.status === "ACTIVE") {
            return '<button class="link-button danger" data-status-user="' + item.id + '" data-status="DISABLED">停用</button>';
        }
        return '<button class="link-button" data-status-user="' + item.id + '" data-status="ACTIVE">' +
            (item.status === "LOCKED" ? "解锁" : "启用") + '</button>';
    }

    function emptyRow(columns) {
        return '<tr><td colspan="' + columns + '"><div class="empty-state"><b>暂无数据</b>可以使用右上角按钮创建第一条记录</div></td></tr>';
    }

    function showToast(message, error) {
        var toast = document.getElementById("toast");
        toast.textContent = message;
        toast.classList.toggle("error", Boolean(error));
        toast.classList.remove("hidden");
        clearTimeout(toastTimer);
        toastTimer = setTimeout(function () { toast.classList.add("hidden"); }, 3200);
    }
})();
