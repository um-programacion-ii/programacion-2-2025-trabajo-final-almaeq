package com.example.backend.event.infrastructure.persistence.repository;

import com.example.backend.event.infrastructure.persistence.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    // Método para traer todos los eventos ordenados por fecha
    List<Event> findAllByOrderByFechaHoraAsc();
}