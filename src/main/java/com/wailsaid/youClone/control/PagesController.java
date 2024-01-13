package com.wailsaid.youClone.control;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.wailsaid.youClone.Video;
import com.wailsaid.youClone.Repo.VideoRepo;
import com.wailsaid.youClone.service.VideoService;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class PagesController {

  @GetMapping
  String getHome(Model m) {

    var videos = vr.findAll();
    m.addAttribute("videos", videos);
    System.out.println(videos);

    return "index";
  }

  @HxRequest
  @GetMapping
  String getHomeContent() {

    return "index :: main";
  }

  @GetMapping("upload")
  String uploadPage() {
    return "upload";
  }

  @GetMapping("video")
  String videoPage() {
    return "video";
  }

  @HxRequest
  @GetMapping("upload")
  String uploadForm() {
    return "upload :: form";
  }

  private final VideoRepo vr;

  @PostMapping("upload")
  String uploadVideo(@RequestParam("title") String title,
      @RequestParam("about") String description,
      @RequestParam("photo-upload") MultipartFile thumnial,
      @RequestParam("file-upload") MultipartFile videofFile)
      throws IOException {

    new Video();
    var video = Video.builder().title(title).description(description).build();

    var res = vr.save(video);

    System.out.println(res.getId().toString());
    System.out.println(videofFile.getResource().toString());

    Path path = Paths.get("./uploads/videos/" + res.getId() + ".mp4");

    Files.copy(videofFile.getInputStream(), path,
        StandardCopyOption.REPLACE_EXISTING);

    return "upload";
  }

    @GetMapping(path = "/watch")
  String watchPae() {


    return "video";
  }

  @GetMapping(path = "/watch/{fileId}")
  String watchPage(@PathVariable("fileId") String id, Model m) {

    m.addAttribute("video", id);
    return "video";
  }

/*   @HxRequest
  @GetMapping(path = "/watch/{fileId}")
  String watchComp(@PathVariable("fileId") Long id, Model m) {

    return "video :: video";
  }
 */
  private final VideoService vservice;

  @GetMapping(path = "/stream3/{id}", produces = "video/mp4")
  @ResponseBody
  public Mono<Resource> stream3(@PathVariable("id") String id, @RequestHeader("Range") String range) {
    System.out.println("range is : " + range);

    return vservice.StreamVideo(id);

  }

}
