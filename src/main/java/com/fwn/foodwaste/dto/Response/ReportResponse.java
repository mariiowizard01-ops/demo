package com.fwn.foodwaste.dto.Response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportResponse {

    private String label;   // donor name OR waste type string
    private Double totalKg;
    private Long   count;
}
