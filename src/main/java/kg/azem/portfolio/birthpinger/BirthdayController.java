package kg.azem.portfolio.birthpinger;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/birthdays")
public class BirthdayController {
    private final BirthdayService service;
    private final BirthdayMessageClient messageClient;
    private final BirthdayScheduler scheduler;
    public BirthdayController(BirthdayService service, BirthdayMessageClient messageClient, BirthdayScheduler scheduler) {
        this.service = service; this.messageClient = messageClient; this.scheduler = scheduler;
    }

    @GetMapping public List<BirthdayPerson> findAll() { return service.findAll(); }
    @GetMapping("/{id:[0-9]+}")
    public BirthdayPerson findById(@PathVariable long id) { return service.findById(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public BirthdayPerson create(@Valid @RequestBody BirthdayRequest request) { return service.create(request); }
    @PutMapping("/{id:[0-9]+}")
    public BirthdayPerson update(@PathVariable long id, @Valid @RequestBody BirthdayRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id:[0-9]+}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { service.delete(id); }
    @PostMapping("/import")
    public ImportResult importRows(@RequestBody @NotEmpty @Size(max = 1000) List<@Valid BirthdayImportRow> rows) {
        int inserted = service.importRows(rows);
        return new ImportResult(inserted, rows.size() - inserted);
    }
    @PostMapping("/telegram/test")
    public TestMessageResult sendTelegramTest() {
        messageClient.sendTestMessage();
        return new TestMessageResult("Тестовое сообщение отправлено");
    }
    @PostMapping("/openai/test")
    public OpenAiTestResult generateOpenAiTest() {
        return new OpenAiTestResult(messageClient.generateTest());
    }
    @PostMapping("/process-today")
    public BirthdayScheduler.ProcessingSummary processToday() {
        return scheduler.processTodayNow();
    }
    @PostMapping("/resend-today")
    public BirthdayScheduler.ProcessingSummary resendToday() {
        return scheduler.resendTodayNow();
    }
    public record ImportResult(int imported, int duplicates) {}
    public record TestMessageResult(String status) {}
    public record OpenAiTestResult(String generatedText) {}
}
