package com.wailsaid.youClone;

import jakarta.annotation.Resources;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class homeController {

  final private videoRepository repository;

  @GetMapping
  String getHome(Model m) {

    m.addAttribute("VideoList", repository.findAll());
    return "index";
  }

  @GetMapping("/home")
  String getHomeContent(Model m) {
    m.addAttribute("VideoList", repository.findAll());
    return "index :: main";
  }

  @GetMapping("upload")
  String uploadPage() {
    return "upload";
  }

  @PostMapping("upload")
  String uploadVideo(@RequestParam("title") String title,
                     @RequestParam("description") String description,
                     @RequestParam("videoBits") MultipartFile videoBits)
      throws IOException {

    Path path =
        Paths.get("./uploads/videos/" + videoBits.getOriginalFilename());

    Files.copy(videoBits.getInputStream(), path,
               StandardCopyOption.REPLACE_EXISTING);

    Video v = Video.builder()
                  .title(title)
                  .description(description)
                  .path(path.toString())
                  .build();

    repository.save(v);

    return "components :: upload-Done";
  }

  @GetMapping(path = "/watch/{fileId}")
  String watchPage(@PathVariable("fileId") Long id, Model m) {

    m.addAttribute("video", repository.findById(id).get());
    return "video";
  }

  @GetMapping(path = "/stream/{fileId}")
  @ResponseBody
  ResponseEntity<StreamingResponseBody>
  videoStream(@PathVariable("fileId") Long id,
              @RequestHeader(value = "Range", required = false) String range)
      throws Exception {
    /*
     * long rangeStart = Long.parseLong(range.replace("bytes=",
     * "").split("-")[0]);
     *
     * long rangeEnd = Long.parseLong(range.replace("bytes=",
     * "").split("-")[1]); InputStream io =
     * Files.newInputStream(Paths.get(v.getPath()), StandardOpenOption.READ);
     * long contentLenght = v.getSize(); // you must have it somewhere stored or
     * // read the full file size
     *
     * HttpHeaders headers = new HttpHeaders();
     * headers.setContentType(MediaType.valueOf("video/mp4"));
     * headers.set("Accept-Ranges", "bytes");
     * headers.set("Expires", "0");
     * headers.set("Cache-Control", "no-cache, no-store");
     * headers.set("Connection", "keep-alive");
     * headers.set("Content-Transfer-Encoding", "binary");
     * headers.set("Content-Length", String.valueOf(rangeEnd - rangeStart + 1));
     *
     * // if start range assume that all content
     * if (rangeStart == 0) {
     * return new ResponseEntity<>(new InputStreamResource(io), headers,
     * HttpStatus.OK);
     * } else {
     * headers.set("Content-Range", String.format("bytes %s-%s/%s", rangeStart,
     * rangeEnd, contentLenght));
     * return new ResponseEntity<>(new InputStreamResource(io), headers,
     * HttpStatus.PARTIAL_CONTENT);
     * }
     */

    try {
      Video v = repository.findById(id).get();
      StreamingResponseBody responseStream;
      String filePathString = v.getPath();
      Path filePath = Paths.get(filePathString);
      Long fileSize = Files.size(filePath);
      byte[] buffer = new byte[1024];
      final HttpHeaders responseHeaders = new HttpHeaders();

      if (range == null) {
        responseHeaders.add("Content-Type", "video/mp4");
        responseHeaders.add("Content-Length", fileSize.toString());
        responseStream = os -> {
          RandomAccessFile file = new RandomAccessFile(filePathString, "r");
          try (file) {
            long pos = 0;
            file.seek(pos);
            while (pos < fileSize - 1) {
              file.read(buffer);
              os.write(buffer);
              pos += buffer.length;
            }
            os.flush();
          } catch (Exception e) {
          }
        };

        return new ResponseEntity<StreamingResponseBody>(
            responseStream, responseHeaders, HttpStatus.OK);
      }

      String[] ranges = range.split("-");
      Long rangeStart = Long.parseLong(ranges[0].substring(6));
      Long rangeEnd;
      if (ranges.length > 1) {
        rangeEnd = Long.parseLong(ranges[1]);
      } else {
        rangeEnd = fileSize - 1;
      }

      if (fileSize < rangeEnd) {
        rangeEnd = fileSize - 1;
      }

      String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
      responseHeaders.add("Content-Type", "video/mp4");
      responseHeaders.add("Content-Length", contentLength);
      responseHeaders.add("Accept-Ranges", "bytes");
      responseHeaders.add("Content-Range", "bytes"
                                               + " " + rangeStart + "-" +
                                               rangeEnd + "/" + fileSize);
      final Long _rangeEnd = rangeEnd;
      responseStream = os -> {
        RandomAccessFile file = new RandomAccessFile(filePathString, "r");
        try (file) {
          long pos = rangeStart;
          file.seek(pos);
          while (pos < _rangeEnd) {
            file.read(buffer);
            os.write(buffer);
            pos += buffer.length;
          }
          os.flush();
        } catch (Exception e) {
        }
      };

      return new ResponseEntity<StreamingResponseBody>(
          responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);
    } catch (FileNotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    } catch (IOException e) {
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}

@Repository
interface videoRepository extends JpaRepository<Video, Long> {}
