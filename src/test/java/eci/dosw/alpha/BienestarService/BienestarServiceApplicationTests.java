package eci.dosw.alpha.BienestarService;

import eci.dosw.alpha.BienestarService.dto.EventDTO;
import eci.dosw.alpha.BienestarService.model.EmergencyContact;
import eci.dosw.alpha.BienestarService.model.Resource;
import eci.dosw.alpha.BienestarService.repository.EmergencyContactRepository;
import eci.dosw.alpha.BienestarService.repository.ResourceRepository;
import eci.dosw.alpha.BienestarService.service.BienestarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BienestarServiceApplicationTests {

    @Mock ResourceRepository resourceRepository;
    @Mock RestTemplate restTemplate;
    @Mock EmergencyContactRepository emergencyContactRepository;

    @InjectMocks BienestarService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "eventsServiceUrl", "http://localhost:8081/events");
    }

    // ── getResources ──────────────────────────────────────────────────────────

    @Test
    void getResources_noCategory_returnsAll() {
        List<Resource> all = List.of(new Resource(), new Resource());
        when(resourceRepository.findAll()).thenReturn(all);

        assertThat(service.getResources(null)).hasSize(2);
        verify(resourceRepository).findAll();
    }

    @Test
    void getResources_withCategory_returnsFiltered() {
        Resource r = new Resource();
        r.setCategory("BIENESTAR");
        when(resourceRepository.findByCategory("BIENESTAR")).thenReturn(List.of(r));

        List<Resource> result = service.getResources("BIENESTAR");

        assertThat(result).hasSize(1);
        verify(resourceRepository).findByCategory("BIENESTAR");
    }

    @Test
    void getResources_blankCategory_treatedAsAll() {
        when(resourceRepository.findAll()).thenReturn(List.of());
        service.getResources("   ");
        verify(resourceRepository, never()).findByCategory(any());
        verify(resourceRepository).findAll();
    }

    @Test
    void getResources_E1_emptyList_returnsEmpty() {
        when(resourceRepository.findAll()).thenReturn(List.of());
        assertThat(service.getResources(null)).isEmpty();
    }

    // ── getWellbeingEvents ────────────────────────────────────────────────────

    @Test
    void getWellbeingEvents_success_returnsList() {
        EventDTO e = new EventDTO();
        e.setCategory("BIENESTAR");
        when(restTemplate.getForObject(anyString(), eq(EventDTO[].class)))
                .thenReturn(new EventDTO[]{e});

        List<EventDTO> result = service.getWellbeingEvents();

        assertThat(result).hasSize(1);
    }

    @Test
    void getWellbeingEvents_E2_serviceDown_returnsEmpty() {
        when(restTemplate.getForObject(anyString(), eq(EventDTO[].class)))
                .thenThrow(new RestClientException("connection refused"));

        List<EventDTO> result = service.getWellbeingEvents();

        assertThat(result).isEmpty();
    }

    @Test
    void getWellbeingEvents_nullResponse_returnsEmpty() {
        when(restTemplate.getForObject(anyString(), eq(EventDTO[].class)))
                .thenReturn(null);

        assertThat(service.getWellbeingEvents()).isEmpty();
    }

    // ── getEmergencyContact ──────────────────────────────────────────────────

    @Test
    void getEmergencyContact_exists_returnsFirst() {
        EmergencyContact c = new EmergencyContact();
        c.setName("Psicología");
        when(emergencyContactRepository.findAll()).thenReturn(List.of(c));

        assertThat(service.getEmergencyContact().getName()).isEqualTo("Psicología");
    }

    @Test
    void getEmergencyContact_none_throws() {
        when(emergencyContactRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.getEmergencyContact())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No hay contacto");
    }

    // ── createEmergencyContact ────────────────────────────────────────────────

    @Test
    void createEmergencyContact_savesAndReturns() {
        EmergencyContact contact = new EmergencyContact();
        contact.setName("Emergencias");
        when(emergencyContactRepository.save(contact)).thenReturn(contact);

        EmergencyContact result = service.createEmergencyContact(contact);

        assertThat(result.getName()).isEqualTo("Emergencias");
        verify(emergencyContactRepository).save(contact);
    }

    // ── getAllEmergencyContacts ────────────────────────────────────────────────

    @Test
    void getAllEmergencyContacts_returnsAll() {
        when(emergencyContactRepository.findAll()).thenReturn(List.of(new EmergencyContact(), new EmergencyContact()));

        assertThat(service.getAllEmergencyContacts()).hasSize(2);
    }
}
