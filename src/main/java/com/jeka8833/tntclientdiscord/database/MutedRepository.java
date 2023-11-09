package com.jeka8833.tntclientdiscord.database;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MutedRepository extends JpaRepository<Muted, UUID> {
}
