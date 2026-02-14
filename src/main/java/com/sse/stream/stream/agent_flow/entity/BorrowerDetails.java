package com.sse.stream.stream.agent_flow.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "borrower_details", schema = "public")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "spocto_id", nullable = true, length = 100)
    private String spoctoId;

    @Column(name = "allocation_records_id", nullable = false)
    private String allocationRecordsId;

    @Column(name = "allocation_id", nullable = false)
    private String allocationId;

    @Column(name = "lender_id", nullable = false, columnDefinition = "text")
    private String lenderId;
}
