package com.zipbob.zipbob_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "tag_mng", schema = "zipbob_app")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_seq")
    private Integer tagSeq;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(name = "code_id", nullable = false, length = 50)
    private String codeID;

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

    // 메뉴와 음식 특징 태그의 연결 생성
    public static Tag create(
            Menu menu,
            String codeId,
            String useYn,
            String adminId
    ){
        Tag tag = new Tag();
        tag.menu = menu;
        tag.codeID = codeId;
        tag.useYn = useYn;
        tag.createdId = adminId;

        return tag;
    }
}
