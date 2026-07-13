package eci.dosw.alpha.BienestarService.service;

import eci.dosw.alpha.BienestarService.dto.EmergencyContactDTO;
import eci.dosw.alpha.BienestarService.dto.EventDTO;
import eci.dosw.alpha.BienestarService.model.EmergencyContact;
import eci.dosw.alpha.BienestarService.model.Resource;
import eci.dosw.alpha.BienestarService.repository.EmergencyContactRepository;
import eci.dosw.alpha.BienestarService.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class BienestarService {

    private final ResourceRepository resourceRepository;
    private final RestTemplate restTemplate;
    private final EmergencyContactRepository emergencyContactRepository;

    @Value("${events.service.url}")
    private String eventsServiceUrl;

    public BienestarService(ResourceRepository resourceRepository,
                            RestTemplate restTemplate,
                            EmergencyContactRepository emergencyContactRepository) {
        this.resourceRepository = resourceRepository;
        this.restTemplate = restTemplate;
        this.emergencyContactRepository = emergencyContactRepository;
    }

    /** E1: sin recursos → retorna lista vacía (estado vacío, no error). */
    public List<Resource> getResources(String category) {
        if (category != null && !category.isBlank()) {
            return resourceRepository.findByCategory(category);
        }
        return resourceRepository.findAll();
    }

    /**
     * E2: eventos no disponibles → retorna lista vacía y el frontend muestra recursos.
     * No propaga la excepción para no romper el Centro de Bienestar.
     */
    public List<EventDTO> getWellbeingEvents() {
        try {
            String url = eventsServiceUrl + "?category=BIENESTAR";
            EventDTO[] response = restTemplate.getForObject(url, EventDTO[].class);
            if (response == null) return Collections.emptyList();
            return Arrays.asList(response);
        } catch (RestClientException e) {
            return Collections.emptyList();
        }
    }

    public EmergencyContact getEmergencyContact() {
        return emergencyContactRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No hay contacto de emergencia configurado"));
    }

    public EmergencyContact createEmergencyContact(EmergencyContactDTO contact) {
        EmergencyContact entity = new EmergencyContact();
        entity.setName(contact.getName());
        entity.setPhone(contact.getPhone());
        entity.setEmail(contact.getEmail());
        return emergencyContactRepository.save(entity);
    }

    public List<EmergencyContact> getAllEmergencyContacts() {
        return emergencyContactRepository.findAll();
    }
}
