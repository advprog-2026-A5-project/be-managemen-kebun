package id.ac.ui.cs.advprog.kebun.service;

import id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent;
import id.ac.ui.cs.advprog.kebun.integration.client.PersonnelDirectory;
import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.repository.KebunRepository;
import id.ac.ui.cs.advprog.kebun.validation.OverlapValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KebunServiceDomainEventPublicationTest {

    @Mock
    private KebunRepository kebunRepository;

    @Mock
    private OverlapValidator overlapValidator;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private PersonnelDirectory personnelDirectory;

    @Test
    void assignMandorShouldPublishMandorAssignedDomainEvent() {
        KebunService kebunService = new KebunService(
                kebunRepository,
                overlapValidator,
                personnelDirectory,
                applicationEventPublisher
        );

        when(kebunRepository.findByCode("KB001")).thenReturn(Optional.of(
                Kebun.builder().name("Kebun A").code("KB001").luas(100.0).coordinates(squarePoints()).build()
        ));

        when(personnelDirectory.requireMandorId("3")).thenReturn("3");

        kebunService.assignMandor("KB001", "3");

        verify(applicationEventPublisher).publishEvent(argThat((Object event) ->
                event instanceof MandorAssignedEvent
                        && "KB001".equals(((MandorAssignedEvent) event).getKebunCode())
                        && "3".equals(((MandorAssignedEvent) event).getMandorId())
        ));
    }

    private java.util.List<Kebun.Point> squarePoints() {
        return java.util.List.of(
                new Kebun.Point(0, 0),
                new Kebun.Point(0, 1),
                new Kebun.Point(1, 1),
                new Kebun.Point(1, 0)
        );
    }
}
