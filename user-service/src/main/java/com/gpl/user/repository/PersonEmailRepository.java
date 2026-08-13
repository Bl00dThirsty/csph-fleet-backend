package com.gpl.user.repository;
import com.gpl.user.model.PersonEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PersonEmailRepository extends JpaRepository<PersonEmail, Integer> {
    List<PersonEmail> findByPersonId(String personId);
    void deleteByPersonIdAndEmailId(String personId, Integer emailId);
}
