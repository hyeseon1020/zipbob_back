package com.zipbob.zipbob_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "code_mng", schema = "zipbob_app")
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Code {

    @Id
    @Column(name = "code_id", length = 50, nullable = false)
    private String codeId;

    @Column(name = "code_name", length = 50, nullable = false)
    private String codeName;

    @Column(name = "code_parent", length = 50)
    private String codeParent;

    @Column(name = "code_depth")
    private Integer codeDepth;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "use_yn", length =  1, columnDefinition = "char(1)")
    private String useYn ="Y";

    @Column(name = "created_id", length = 50)
    private String createdId;

    @Column(name = "crated_dt", insertable = false, updatable = false)
    private LocalDateTime createdDt;

    @Column(name = "updated_id", length = 50)
    private String updatedId;

    @Column(name = "updated_dt")
    private LocalDateTime updatedDt;
}
