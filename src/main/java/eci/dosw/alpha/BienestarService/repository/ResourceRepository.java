package eci.dosw.alpha.BienestarService.repository;

import eci.dosw.alpha.BienestarService.model.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ResourceRepository extends MongoRepository<Resource, String> {

    List<Resource> findByCategory(String category);

}

