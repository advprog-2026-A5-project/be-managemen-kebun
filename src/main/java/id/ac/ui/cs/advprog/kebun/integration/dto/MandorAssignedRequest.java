package id.ac.ui.cs.advprog.kebun.integration.dto;

import id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent;

public record MandorAssignedRequest(String kebunCode, String mandorId) {

    public static MandorAssignedRequest fromEvent(MandorAssignedEvent event) {
        return new MandorAssignedRequest(event.getKebunCode(), event.getMandorId());
    }
}
