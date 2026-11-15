package com.umeed.reports;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record CreateReportRequest(
        @NotNull(message = "Reason is required")
        Report.Reason reason,

        @Size(max = 500, message = "Details must be at most 500 characters")
        String details) {
}
