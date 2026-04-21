package com.africa.ubaxplatform.contract.repository;

import com.africa.ubaxplatform.contract.entity.Contract;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {}
