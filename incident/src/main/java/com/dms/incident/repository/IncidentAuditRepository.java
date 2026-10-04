package com.dms.incident.repository;

import com.dms.incident.entity.IncidentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface IncidentAuditRepository extends JpaRepository<IncidentAudit, UUID> { }
