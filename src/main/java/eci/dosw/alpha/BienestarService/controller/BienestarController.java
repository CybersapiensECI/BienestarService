package eci.dosw.alpha.BienestarService.controller;

import eci.dosw.alpha.BienestarService.dto.EventDTO;
import eci.dosw.alpha.BienestarService.model.EmergencyContact;
import eci.dosw.alpha.BienestarService.model.Resource;
import eci.dosw.alpha.BienestarService.service.BienestarService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bienestar")
public class BienestarController {

    private final BienestarService bienestarService;

    public BienestarController(BienestarService bienestarService) {
        this.bienestarService = bienestarService;
    }

    // 1. Obtener recursos de bienestar
    @GetMapping("/resources")
    public List<Resource> getResources(@RequestParam(required = false) String category) {
        return bienestarService.getResources(category);
    }

    // 2. Obtener eventos de bienestar (desde events-service)
    @GetMapping("/events")
    public List<EventDTO> getEvents() {
        return bienestarService.getWellbeingEvents();
    }

    // 3. Obtener contacto principal de emergencia
    @GetMapping("/contact")
    public EmergencyContact getContact() {
        return bienestarService.getEmergencyContact();
    }

    // 4. Crear contacto de emergencia
    @PostMapping("/contact")
    public EmergencyContact createContact(@RequestBody EmergencyContact contact) {
        return bienestarService.createEmergencyContact(contact);
    }

    // 5.) Listar todos los contactos
    @GetMapping("/contacts")
    public List<EmergencyContact> getAllContacts() {
        return bienestarService.getAllEmergencyContacts();
    }
}
