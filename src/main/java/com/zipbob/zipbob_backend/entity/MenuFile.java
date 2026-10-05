package com.zipbob.zipbob_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_mng", schema = "zipbob_app")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Integer fileId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "file_save_name", nullable = false, length = 255)
    private String fileSaveName;

    @Column(name = "file_name", nullable = false, length = 100)
    private String fileName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "use_yn", length = 1, columnDefinition = "char(1)")
    private String useYn = "Y";

    @Column(name = "created_id", length = 50)
    private String createdId;

    @Column(name = "crated_dt", insertable = false, updatable = false)
    private LocalDateTime createdDt;

    @Column(name = "updated_id", length = 50)
    private String updatedId;

    @Column(name = "updated_dt")
    private LocalDateTime updatedDt;
}