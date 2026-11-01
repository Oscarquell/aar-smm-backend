package kg.azem.portfolio.applications;

import org.springframework.stereotype.Service;

@Service
public class BlockedIpService {
    private final BlockedIpRepository repository;

    public BlockedIpService(BlockedIpRepository repository) {
        this.repository = repository;
    }

    public boolean isBlocked(String ip) {
        return repository.isBlocked(ip);
    }

    public void block(String ip) {
        repository.block(ip);
    }

    public int releaseExpiredBlocks() {
        return repository.releaseExpiredBlocks();
    }
}
