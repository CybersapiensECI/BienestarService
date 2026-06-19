package eci.dosw.alpha.BienestarService.dto;

import lombok.Data;

@Data
public class EventDTO {

    private String id;
    private String name;
    private String description;
    private String category;
    private String date;
    private int availableCapacity;
}

