package com.superhumans.repository;

import com.superhumans.entity.MedicineCatalogCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MedicineCatalogCacheRepository extends JpaRepository<MedicineCatalogCache, Long> {
    List<MedicineCatalogCache> findByNameContainingIgnoreCase(String name);
}
