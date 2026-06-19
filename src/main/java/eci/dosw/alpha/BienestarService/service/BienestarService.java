package eci.dosw.alpha.BienestarService.service;


import eci.dosw.alpha.BienestarService.dto.EventDTO;
import eci.dosw.alpha.BienestarService.model.EmergencyContact;
import eci.dosw.alpha.BienestarService.model.Resource;
import eci.dosw.alpha.BienestarService.repository.ResourceRepository;
import eci.dosw.alpha.BienestarService.repository.EmergencyContactRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
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


    // Obtener recursos
    public List<Resource> getResources(String category) {

        if (category != null) {
            return resourceRepository.findByCategory(category);
        }

        return resourceRepository.findAll();
    }

    // Obtener eventos de bienestar (desde events-service)
    public List<EventDTO> getWellbeingEvents() {

        String url = eventsServiceUrl + "?category=BIENESTAR";

        EventDTO[] response = restTemplate.getForObject(url, EventDTO[].class);

        return Arrays.asList(response);
    }

    // Contacto de emergencia

    public EmergencyContact getEmergencyContact() {

        return emergencyContactRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No hay contacto de emergencia configurado"));
    }

    // Crear contacto
    public EmergencyContact createEmergencyContact(EmergencyContact contact) {
        return emergencyContactRepository.save(contact);
    }

    // Obtener todos los contactos
    public List<EmergencyContact> getAllEmergencyContacts() {
        return emergencyContactRepository.findAll();
    }

}
