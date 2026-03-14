package com.goservi.zone.repository;

import com.goservi.zone.entity.ZoneRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneRequestRepository extends JpaRepository<ZoneRequest, Long> {
    List<ZoneRequest> findByAttendedFalse();
}
