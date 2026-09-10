package com.bankops.model;

import com.bankops.service.BusinessException;
import org.springframework.http.HttpStatus;

public enum ManagementModule {
    ARCHITECTURE("ARC"),
    OUTSOURCING("OUT"),
    PROJECT("PRJ"),
    REQUIREMENT("REQ"),
    CHANGE_RELEASE("CHG"),
    ASSET_RESOURCE("RES"),
    PERSONNEL("PER"),
    DOCUMENT("DOC"),
    RISK_COMPLIANCE("RSK");

    private final String prefix;

    ManagementModule(String prefix) { this.prefix = prefix; }

    public String prefix() { return prefix; }

    public static ManagementModule fromPath(String value) {
        try {
            return ManagementModule.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持的管理模块");
        }
    }
}
