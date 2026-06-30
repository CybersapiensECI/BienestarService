package eci.dosw.alpha.BienestarService.controller;

import eci.dosw.alpha.BienestarService.dto.EventDTO;
import eci.dosw.alpha.BienestarService.model.EmergencyContact;
import eci.dosw.alpha.BienestarService.model.Resource;
import eci.dosw.alpha.BienestarService.service.BienestarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bienestar", description = "Centro de Bienestar Universitario: recursos, eventos y contactos de emergencia")
@RestController
@RequestMapping("/bienestar")
public class BienestarController {

    private final BienestarService bienestarService;

    public BienestarController(BienestarService bienestarService) {
        this.bienestarService = bienestarService;
    }

    @Operation(summary = "Listar recursos de bienestar",
               description = "Devuelve recursos disponibles. E1: si no hay recursos → lista vacía (no es error).")
    @ApiResponse(responseCode = "200", description = "Lista de recursos (puede ser vacía)")
    @GetMapping("/resources")
    public List<Resource> getResources(
            @Parameter(description = "Filtrar por categoría (ej. PSICOLOGÍA, NUTRICIÓN)")
            @RequestParam(required = false) String category) {
        return bienestarService.getResources(category);
    }

    @Operation(summary = "Obtener eventos de bienestar",
               description = "Consulta eventos con categoría BIENESTAR desde EventService. E2: si EventService no está disponible → lista vacía.")
    @ApiResponse(responseCode = "200", description = "Lista de eventos de bienestar (puede ser vacía si EventService no responde)")
    @GetMapping("/events")
    public List<EventDTO> getEvents() {
        return bienestarService.getWellbeingEvents();
    }

    @Operation(summary = "Obtener contacto de emergencia principal",
               description = "Retorna el primer contacto de emergencia configurado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contacto de emergencia encontrado"),
        @ApiResponse(responseCode = "500", description = "No hay contacto de emergencia configurado")
    })
    @GetMapping("/contact")
    public EmergencyContact getContact() {
        return bienestarService.getEmergencyContact();
    }

    @Operation(summary = "Crear contacto de emergencia",
               description = "Registra un nuevo contacto de emergencia en la base de datos.")
    @ApiResponse(responseCode = "200", description = "Contacto creado")
    @PostMapping("/contact")
    public EmergencyContact createContact(@RequestBody EmergencyContact contact) {
        return bienestarService.createEmergencyContact(contact);
    }

    @Operation(summary = "Listar todos los contactos de emergencia")
    @ApiResponse(responseCode = "200", description = "Lista de contactos de emergencia")
    @GetMapping("/contacts")
    public List<EmergencyContact> getAllContacts() {
        return bienestarService.getAllEmergencyContacts();
    }
}
