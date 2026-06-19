package eci.dosw.alpha.BienestarService.repository;


import eci.dosw.alpha.BienestarService.model.EmergencyContact;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmergencyContactRepository extends MongoRepository<EmergencyContact, String> {
}

