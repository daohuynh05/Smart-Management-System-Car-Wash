package com.autowash.autowash_pro.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.autowash.autowash_pro.entity.BookingWashService;

public interface BookingWashServiceRepository extends JpaRepository<BookingWashService, UUID> {
}
