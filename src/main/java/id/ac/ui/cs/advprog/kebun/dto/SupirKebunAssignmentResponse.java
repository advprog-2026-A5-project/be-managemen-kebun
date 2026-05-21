package id.ac.ui.cs.advprog.kebun.dto;

public record SupirKebunAssignmentResponse(
        Long supirId,
        String kebunId,
        String kebunCode,
        String kebunName,
        boolean active
) {
}
