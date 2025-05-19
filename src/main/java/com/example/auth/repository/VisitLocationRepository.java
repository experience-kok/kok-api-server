package com.example.auth.repository;

import com.example.auth.domain.VisitLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitLocationRepository extends JpaRepository<VisitLocation, Long> {
    List<VisitLocation> findByCampaignId(Long campaignId);
}
