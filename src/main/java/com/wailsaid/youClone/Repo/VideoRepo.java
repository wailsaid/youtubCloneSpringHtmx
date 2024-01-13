package com.wailsaid.youClone.Repo;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.wailsaid.youClone.Video;

@Repository
public interface VideoRepo extends CrudRepository<Video, UUID> {

}
