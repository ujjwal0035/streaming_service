package com.sse.stream.stream.agent_flow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sse.stream.stream.agent_flow.entity.BorrowerDetails;


public interface BorrowerRepository extends JpaRepository<BorrowerDetails, Long>{

    // Custom query to find records based on your callback requirement
    List<BorrowerDetails> findByAllocationRecordsId(String allocationRecordsId);
    List<BorrowerDetails> findByLenderIdAndAllocationIdAndSpoctoId(String lenderId, String allocationId, String spoctoId);
}
