package com.wailsaid.youClone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Video {
 private Long id;

  private String title;

  private String description;

  private long size;

  private String path;
}
