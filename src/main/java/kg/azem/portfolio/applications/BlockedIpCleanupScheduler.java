package kg.azem.portfolio.applications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BlockedIpCleanupScheduler {
    private static final Logger logger = LoggerFactory.getLogger(BlockedIpCleanupScheduler.class);

    private final BlockedIpService blockedIpService;

    public BlockedIpCleanupScheduler(BlockedIpService blockedIpService) {
        this.blockedIpService = blockedIpService;
    }

    @Scheduled(cron = "0 */30 * * * *", zone = "UTC")
    public void releaseExpiredBlocks() {
        int released = blockedIpService.releaseExpiredBlocks();
        if (released > 0) {
            logger.info("Released {} expired IP block(s)", released);
        }
    }
}
