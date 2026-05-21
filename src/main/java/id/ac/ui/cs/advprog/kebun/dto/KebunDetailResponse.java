package id.ac.ui.cs.advprog.kebun.dto;

import id.ac.ui.cs.advprog.kebun.model.Kebun;

import java.util.List;

public record KebunDetailResponse(
        String code,
        String name,
        double luas,
        List<Kebun.Point> coordinates,
        String mandorId,
        List<String> supirIds
) {
}
