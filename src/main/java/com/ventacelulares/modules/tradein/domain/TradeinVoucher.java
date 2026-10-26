package com.ventacelulares.modules.tradein.domain;

import com.ventacelulares.shared.domain.Sucursal;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;

public class TradeinVoucher {
    public record Device(String brand, String model, int memoryGb) {}
    public record Checklist(boolean powersOn, boolean screenIntact, int batteryPercent, boolean accountUnlocked, boolean boxAndCharger) {}
    public enum Status { AVAILABLE, CONFIRMED }
    public enum ApprovalStatus { PENDING, APPROVED, REJECTED }
    public record Approval(String id, String seller, Sucursal branch, String imei, BigDecimal estimate, BigDecimal proposed, BigDecimal deviationPercent, String reason, Integer batteryPercent, ApprovalStatus status) {}

    private final String code;
    private final Sucursal branch;
    private final Device declaredDevice;
    private final Checklist declaredChecklist;
    private final ZonedDateTime issuedAt;
    private final ZonedDateTime expiresAt;
    private Device verifiedDevice;
    private Checklist verifiedChecklist;
    private BigDecimal estimate;
    private Approval approval;
    private Status status = Status.AVAILABLE;
    private String confirmedImei;
    private BigDecimal finalValue;

    public TradeinVoucher(String code, Sucursal branch, Device device, Checklist checklist, BigDecimal estimate, ZonedDateTime issuedAt) {
        this.code = Objects.requireNonNull(code);
        this.branch = Objects.requireNonNull(branch);
        this.declaredDevice = Objects.requireNonNull(device);
        this.declaredChecklist = Objects.requireNonNull(checklist);
        this.estimate = Objects.requireNonNull(estimate);
        this.issuedAt = Objects.requireNonNull(issuedAt);
        this.expiresAt = issuedAt.plusHours(72);
    }

    public String code() { return code; }
    public Sucursal branch() { return branch; }
    public Device declaredDevice() { return declaredDevice; }
    public Checklist declaredChecklist() { return declaredChecklist; }
    public Device verifiedDevice() { return verifiedDevice; }
    public Checklist verifiedChecklist() { return verifiedChecklist; }
    public Device currentDevice() { return verifiedDevice == null ? declaredDevice : verifiedDevice; }
    public Checklist currentChecklist() { return verifiedChecklist == null ? declaredChecklist : verifiedChecklist; }
    public BigDecimal estimate() { return estimate; }
    public ZonedDateTime issuedAt() { return issuedAt; }
    public ZonedDateTime expiresAt() { return expiresAt; }
    public Approval approval() { return approval; }
    public Status status() { return status; }
    public String confirmedImei() { return confirmedImei; }
    public BigDecimal finalValue() { return finalValue; }

    public void update(Device device, Checklist checklist, BigDecimal value) {
        verifiedDevice = device;
        verifiedChecklist = checklist;
        estimate = value;
        approval = null;
    }

    public void setApproval(Approval value) { approval = value; }

    public void confirm(String imei, BigDecimal value) {
        confirmedImei = imei;
        finalValue = value;
        status = Status.CONFIRMED;
    }
}
