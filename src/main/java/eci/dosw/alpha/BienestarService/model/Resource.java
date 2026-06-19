package eci.dosw.alpha.BienestarService.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "resources")
public class Resource {

    @Id
    private String id;

    private String title;
    private String description;
    private String type; // TIP, ARTICLE, CONTACT
    private String category;
}

