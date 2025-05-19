package com.example.demo.repository;


import com.example.demo.model.Organizzazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizzazioneRepository extends JpaRepository<Organizzazione, Long> {

}
