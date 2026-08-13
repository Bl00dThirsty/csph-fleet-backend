package com.gpl.user.repository;
import com.gpl.user.model.PersonPhone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PersonPhoneRepository extends JpaRepository<PersonPhone, Integer> {
    List<PersonPhone> findByPersonId(String personId);
    void deleteByPersonIdAndPhoneId(String personId, Integer phoneId);
}
