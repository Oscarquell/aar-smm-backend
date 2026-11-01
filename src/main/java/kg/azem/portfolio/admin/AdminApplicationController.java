package kg.azem.portfolio.admin;

import jakarta.validation.Valid;
import java.util.List;
import kg.azem.portfolio.applications.ApplicationRecord;
import kg.azem.portfolio.applications.ApplicationService;
import kg.azem.portfolio.applications.UpdateApplicationRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/applications")
public class AdminApplicationController {
    private final ApplicationService service;

    public AdminApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApplicationRecord> getAll() {
        return service.getAll();
    }

    @PatchMapping("/{id}")
    public ApplicationRecord update(@PathVariable long id, @Valid @RequestBody UpdateApplicationRequest request) {
        return service.update(id, request);
    }
}
