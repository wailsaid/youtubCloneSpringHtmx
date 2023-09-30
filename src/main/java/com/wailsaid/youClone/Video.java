package com.wailsaid.youClone;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Entity
public class Video {

    @Id
    @GeneratedValue
    private Long id;

    private String title;

    private String description;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] videoBits;
}
