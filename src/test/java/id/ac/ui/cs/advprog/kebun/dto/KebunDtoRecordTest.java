package id.ac.ui.cs.advprog.kebun.dto;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KebunDtoRecordTest {

    @Test
    void kebunDetailResponseShouldExposeAllFields() {
        List<Kebun.Point> points = List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 2),
                new Kebun.Point(2, 2),
                new Kebun.Point(2, 0)
        );
        KebunDetailResponse response = new KebunDetailResponse(
                "KB001",
                "Kebun A",
                100.5,
                points,
                "10",
                List.of("21", "22")
        );

        assertEquals("KB001", response.code());
        assertEquals("Kebun A", response.name());
        assertEquals(100.5, response.luas());
        assertEquals(4, response.coordinates().size());
        assertEquals("10", response.mandorId());
        assertEquals(List.of("21", "22"), response.supirIds());
    }

    @Test
    void assignmentRequestRecordsShouldExposePayloadFields() {
        MandorAssignmentRequest mandorAssignment = new MandorAssignmentRequest("7");
        MandorReassignmentRequest mandorReassignment = new MandorReassignmentRequest("7", "KB002");
        SupirAssignmentRequest supirAssignment = new SupirAssignmentRequest("11");
        SupirReassignmentRequest supirReassignment = new SupirReassignmentRequest("11", "KB003");
        MandorKebunAssignmentResponse mandorAssignmentResponse =
                new MandorKebunAssignmentResponse(7L, null, "KB002", "Kebun B", true);
        SupirKebunAssignmentResponse supirAssignmentResponse =
                new SupirKebunAssignmentResponse(11L, null, "KB003", "Kebun C", true);

        assertEquals("7", mandorAssignment.mandorId());
        assertEquals("7", mandorReassignment.mandorId());
        assertEquals("KB002", mandorReassignment.replacementKebunCode());
        assertEquals("11", supirAssignment.supirId());
        assertEquals("11", supirReassignment.supirId());
        assertEquals(7L, mandorAssignmentResponse.mandorId());
        assertEquals("KB002", mandorAssignmentResponse.kebunCode());
        assertEquals(11L, supirAssignmentResponse.supirId());
        assertEquals("KB003", supirAssignmentResponse.kebunCode());
        assertTrue(supirReassignment.replacementKebunCode().startsWith("KB"));
    }
}
