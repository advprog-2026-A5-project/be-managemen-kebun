package id.ac.ui.cs.advprog.kebun.dto;

public record MandorKebunAssignmentResponse(
        Long mandorId,
        String kebunId,
        String kebunCode,
        String kebunName,
        boolean active
) {
}
