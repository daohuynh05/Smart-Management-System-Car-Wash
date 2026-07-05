package com.autowash.autowash_pro.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.autowash.autowash_pro.entity.BookingWashService;

@Repository
public interface BookingWashServiceRepository extends JpaRepository<BookingWashService, UUID> {
}
