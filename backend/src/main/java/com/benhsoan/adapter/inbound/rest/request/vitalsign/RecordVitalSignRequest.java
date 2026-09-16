package com.benhsoan.adapter.inbound.rest.request.vitalsign;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordVitalSignRequest {

    @NotNull(message = "Mã lượt khám không được để trống")
    private UUID visitId;

    @Min(value = 30, message = "Mạch tối thiểu 30 lần/phút")
    @Max(value = 250, message = "Mạch tối đa 250 lần/phút")
    private Integer pulse;

    @Min(value = 50, message = "Huyết áp tâm thu tối thiểu 50 mmHg")
    @Max(value = 260, message = "Huyết áp tâm thu tối đa 260 mmHg")
    private Integer bloodPressureSystolic;

    @Min(value = 30, message = "Huyết áp tâm trương tối thiểu 30 mmHg")
    @Max(value = 150, message = "Huyết áp tâm trương tối đa 150 mmHg")
    private Integer bloodPressureDiastolic;

    @DecimalMin(value = "30.0", message = "Nhiệt độ ngoài khoảng hợp lệ (30.0 - 45.0 °C)")
    @DecimalMax(value = "45.0", message = "Nhiệt độ ngoài khoảng hợp lệ (30.0 - 45.0 °C)")
    private BigDecimal temperature;

    @Min(value = 5, message = "Nhịp thở tối thiểu 5 lần/phút")
    @Max(value = 60, message = "Nhịp thở tối đa 60 lần/phút")
    private Integer respiratoryRate;

    @DecimalMin(value = "0.5", message = "Cân nặng tối thiểu 0.5 kg")
    @DecimalMax(value = "300.0", message = "Cân nặng tối đa 300.0 kg")
    private BigDecimal weight;

    @DecimalMin(value = "20.0", message = "Chiều cao tối thiểu 20.0 cm")
    @DecimalMax(value = "250.0", message = "Chiều cao tối đa 250.0 cm")
    private BigDecimal height;

    @Min(value = 50, message = "SpO2 tối thiểu 50%")
    @Max(value = 100, message = "SpO2 tối đa 100%")
    private Integer spo2;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;
}
