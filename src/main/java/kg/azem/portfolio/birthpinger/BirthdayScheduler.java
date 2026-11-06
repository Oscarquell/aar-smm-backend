package kg.azem.portfolio.birthpinger;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientResponseException;

@Component
public class BirthdayScheduler {
    private static final Logger log = LoggerFactory.getLogger(BirthdayScheduler.class);
    private final BirthdayRepository repository;
    private final BirthdayMessageClient client;
    private final String zone;
    public BirthdayScheduler(BirthdayRepository repository, BirthdayMessageClient client,
                             @Value("${app.birthpinger.scheduler.zone:Asia/Bishkek}") String zone) {
        this.repository = repository; this.client = client; this.zone = zone;
    }

    @Scheduled(cron = "${app.birthpinger.scheduler.cron:0 0 9 * * *}", zone = "${app.birthpinger.scheduler.zone:Asia/Bishkek}")
    public void processToday() {
        processTodayNow();
    }

    public ProcessingSummary processTodayNow() {
        return processTodayNow(false);
    }

    public ProcessingSummary resendTodayNow() {
        return processTodayNow(true);
    }

    private ProcessingSummary processTodayNow(boolean resend) {
        LocalDate today = LocalDate.now(java.time.ZoneId.of(zone));
        int sent = 0;
        int failed = 0;
        int skipped = 0;
        for (BirthdayPerson person : repository.findBirthdays(today)) {
            boolean claimed = resend
                    ? repository.claimForResend(person.id(), today.getYear())
                    : repository.claim(person.id(), today.getYear());
            if (!claimed) { skipped++; continue; }
            try {
                String message;
                try {
                    message = client.generate(person, today);
                } catch (OpenAiCreditsExhaustedException noCredits) {
                    message = client.creditsExhaustedMessage();
                    log.warn("OpenAI credits exhausted; sending billing notice for person id={}", person.id());
                }
                client.send(person, message, today);
                repository.markSent(person.id(), today.getYear(), message);
                log.info("Birthday notification sent for person id={}", person.id());
                sent++;
            } catch (Exception error) {
                repository.markFailed(person.id(), today.getYear(), error.getMessage());
                String errorType = error instanceof RestClientResponseException responseError
                        ? "HTTP_" + responseError.getStatusCode().value()
                        : error.getClass().getSimpleName();
                log.error("Birthday notification failed for person id={}, errorType={}", person.id(), errorType);
                failed++;
            }
        }
        return new ProcessingSummary(sent, failed, skipped);
    }

    public record ProcessingSummary(int sent, int failed, int skipped) {}
}
