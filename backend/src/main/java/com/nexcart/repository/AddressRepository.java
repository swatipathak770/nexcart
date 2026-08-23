package com.nexcart.repository;

import com.nexcart.entity.Address;
import com.nexcart.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<Address> findByUser(User user);

    @EntityGraph(attributePaths = {"user"})
    Optional<Address> findById(Long id);

    Optional<Address> findByUserAndIsDefaultTrue(User user);

    boolean existsByIdAndUser(Long id, User user);
}
