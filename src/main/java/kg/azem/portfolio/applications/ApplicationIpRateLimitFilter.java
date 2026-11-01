package kg.azem.portfolio.applications;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApplicationIpRateLimitFilter extends OncePerRequestFilter {
    private static final String APPLICATIONS_PATH = "/api/v1/applications";
    private static final int MAX_REQUESTS = 3;
    private static final int MAX_TRACKED_IPS = 50_000;
    private static final long WINDOW_NANOS = 60L * 60 * 1_000_000_000;
    private static final long CLEANUP_EVERY = 256;

    private final Map<String, Deque<Long>> requestsByIp = new HashMap<>();
    private final BlockedIpService blockedIpService;
    private long acceptedRequests;

    public ApplicationIpRateLimitFilter(BlockedIpService blockedIpService) {
        this.blockedIpService = blockedIpService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !APPLICATIONS_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String clientIp = request.getRemoteAddr();
        if (clientIp == null || clientIp.isBlank()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"Не удалось определить адрес клиента. Попробуйте позже.\"}");
            return;
        }

        if (blockedIpService.isBlocked(clientIp)) {
            writeRateLimitResponse(response, 3600);
            return;
        }

        RateLimitDecision decision = registerRequest(clientIp);

        if (!decision.allowed()) {
            if (!decision.persistBlock()) {
                writeCapacityResponse(response, decision.retryAfterSeconds());
                return;
            }
            blockedIpService.block(clientIp);
            writeRateLimitResponse(response, Math.max(3600, decision.retryAfterSeconds()));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeRateLimitResponse(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"С этого IP уже отправлено 3 заявки за последний час. Попробуйте позже.\"}");
    }

    private void writeCapacityResponse(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"Сервис временно перегружен. Попробуйте позже.\"}");
    }

    private synchronized RateLimitDecision registerRequest(String clientIp) {
        long now = System.nanoTime();
        Deque<Long> requestTimes = requestsByIp.get(clientIp);

        if (requestTimes == null) {
            if (requestsByIp.size() >= MAX_TRACKED_IPS) {
                removeExpiredIpBuckets(now);
                if (requestsByIp.size() >= MAX_TRACKED_IPS) {
                    return new RateLimitDecision(false, 60, false);
                }
            }
            requestTimes = new ArrayDeque<>();
            requestsByIp.put(clientIp, requestTimes);
        }

        removeExpiredRequests(requestTimes, now);
        if (requestTimes.size() >= MAX_REQUESTS) {
            long waitNanos = WINDOW_NANOS - (now - requestTimes.peekFirst());
            long retryAfterSeconds = Math.max(1, (waitNanos + 999_999_999L) / 1_000_000_000L);
            return new RateLimitDecision(false, retryAfterSeconds, true);
        }

        requestTimes.addLast(now);
        if (++acceptedRequests % CLEANUP_EVERY == 0) {
            removeExpiredIpBuckets(now);
        }
        return new RateLimitDecision(true, 0, false);
    }

    private void removeExpiredIpBuckets(long now) {
        Iterator<Map.Entry<String, Deque<Long>>> iterator = requestsByIp.entrySet().iterator();
        while (iterator.hasNext()) {
            Deque<Long> requestTimes = iterator.next().getValue();
            removeExpiredRequests(requestTimes, now);
            if (requestTimes.isEmpty()) iterator.remove();
        }
    }

    private void removeExpiredRequests(Deque<Long> requestTimes, long now) {
        while (!requestTimes.isEmpty() && now - requestTimes.peekFirst() >= WINDOW_NANOS) {
            requestTimes.removeFirst();
        }
    }

    private record RateLimitDecision(boolean allowed, long retryAfterSeconds, boolean persistBlock) {
    }
}
