package eci.dosw.alpha.BienestarService;

import eci.dosw.alpha.BienestarService.controller.BienestarController;
import eci.dosw.alpha.BienestarService.dto.EventDTO;
import eci.dosw.alpha.BienestarService.model.EmergencyContact;
import eci.dosw.alpha.BienestarService.model.Resource;
import eci.dosw.alpha.BienestarService.service.BienestarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BienestarControllerTest {

    @Mock BienestarService bienestarService;
    @InjectMocks BienestarController controller;

    private Resource resource;
    private EventDTO event;
    private EmergencyContact contact;

    @BeforeEach
    void setUp() {
        resource = new Resource();
        resource.setTitle("Apoyo psicológico");

        event = new EventDTO();
        event.setName("Taller de bienestar");

        contact = new EmergencyContact();
        contact.setName("Línea de crisis");
    }

    @Test
    void getResources_noCategory_delegatesToService() {
        when(bienestarService.getResources(null)).thenReturn(List.of(resource));

        List<Resource> result = controller.getResources(null);

        assertThat(result).hasSize(1).contains(resource);
        verify(bienestarService).getResources(null);
    }

    @Test
    void getResources_withCategory_passesCategory() {
        when(bienestarService.getResources("BIENESTAR")).thenReturn(List.of(resource));

        List<Resource> result = controller.getResources("BIENESTAR");

        assertThat(result).hasSize(1);
        verify(bienestarService).getResources("BIENESTAR");
    }

    @Test
    void getEvents_delegatesToService() {
        when(bienestarService.getWellbeingEvents()).thenReturn(List.of(event));

        List<EventDTO> result = controller.getEvents();

        assertThat(result).hasSize(1).contains(event);
    }

    @Test
    void getContact_delegatesToService() {
        when(bienestarService.getEmergencyContact()).thenReturn(contact);

        EmergencyContact result = controller.getContact();

        assertThat(result.getName()).isEqualTo("Línea de crisis");
    }

    @Test
    void createContact_delegatesToService() {
        when(bienestarService.createEmergencyContact(contact)).thenReturn(contact);

        EmergencyContact result = controller.createContact(contact);

        assertThat(result).isEqualTo(contact);
        verify(bienestarService).createEmergencyContact(contact);
    }

    @Test
    void getAllContacts_delegatesToService() {
        when(bienestarService.getAllEmergencyContacts()).thenReturn(List.of(contact));

        List<EmergencyContact> result = controller.getAllContacts();

        assertThat(result).hasSize(1);
    }
}
