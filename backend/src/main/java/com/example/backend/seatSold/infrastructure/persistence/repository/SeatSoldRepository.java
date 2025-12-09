package com.example.backend.seatSold.infrastructure.persistence.repository;

import com.example.backend.seatSold.infrastructure.persistence.entity.SeatSold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatSoldRepository extends JpaRepository<SeatSold, Long> {
}