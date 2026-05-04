package id.ac.ui.cs.advprog.kebun.event;

public class MandorAssignedEvent {
    private final String kebunCode;
    private final String mandorId;

    public MandorAssignedEvent(String kebunCode, String mandorId) {
        this.kebunCode = kebunCode;
        this.mandorId = mandorId;
    }

    public String getKebunCode() {
        return kebunCode;
    }

    public String getMandorId() {
        return mandorId;
    }
}
