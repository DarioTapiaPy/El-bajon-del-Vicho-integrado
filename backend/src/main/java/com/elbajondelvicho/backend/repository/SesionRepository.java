
package com.elbajondelvicho.backend.repository;

import com.elbajondelvicho.backend.entity.Sesion;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface SesionRepository
        extends JpaRepository<Sesion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Sesion> findByTokenHash(String tokenHash);
}
