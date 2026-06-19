package eci.dosw.alpha.BienestarService.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "emergency_contacts")
public class EmergencyContact {

    @Id
    private String id;

    private String name;
    private String phone;
    private String email;
}


