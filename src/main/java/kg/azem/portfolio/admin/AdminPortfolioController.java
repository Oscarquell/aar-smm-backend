package kg.azem.portfolio.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import kg.azem.portfolio.portfolio.PortfolioItem;
import kg.azem.portfolio.portfolio.PortfolioMedia;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/api/v1/admin/portfolio")
public class AdminPortfolioController {
    private final AdminPortfolioService service;

    public AdminPortfolioController(AdminPortfolioService service) {
        this.service = service;
    }

    @GetMapping
    public List<PortfolioItem> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public PortfolioItem get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioItem create(@Valid @RequestBody AdminPortfolioRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PortfolioItem update(@PathVariable long id, @Valid @RequestBody AdminPortfolioRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }

    @PostMapping(value = "/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioMedia uploadMedia(
            @PathVariable long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "") @Size(max = 300) String altText,
            @RequestParam(required = false) String posterUrl,
            @RequestParam(defaultValue = "0") @Min(0) int sortOrder
    ) {
        return service.uploadMedia(id, file, altText, posterUrl, sortOrder);
    }

    @PutMapping("/{id}/media/{mediaId}")
    public PortfolioMedia updateMedia(
            @PathVariable long id,
            @PathVariable long mediaId,
            @Valid @RequestBody AdminMediaUpdateRequest request
    ) {
        return service.updateMedia(id, mediaId, request);
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(@PathVariable long id, @PathVariable long mediaId) {
        service.deleteMedia(id, mediaId);
    }
}
