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
    private long acceptedRequests;

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
        RateLimitDecision decision = registerRequest(clientIp == null ? "unknown" : clientIp);

        if (!decision.allowed()) {
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"С этого IP уже отправлено 3 заявки за последний час. Попробуйте позже.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private synchronized RateLimitDecision registerRequest(String clientIp) {
        long now = System.nanoTime();
        Deque<Long> requestTimes = requestsByIp.get(clientIp);

        if (requestTimes == null) {
            if (requestsByIp.size() >= MAX_TRACKED_IPS) {
                removeExpiredIpBuckets(now);
                if (requestsByIp.size() >= MAX_TRACKED_IPS) {
                    return new RateLimitDecision(false, 60);
                }
            }
            requestTimes = new ArrayDeque<>();
            requestsByIp.put(clientIp, requestTimes);
        }

        removeExpiredRequests(requestTimes, now);
        if (requestTimes.size() >= MAX_REQUESTS) {
            long waitNanos = WINDOW_NANOS - (now - requestTimes.peekFirst());
            long retryAfterSeconds = Math.max(1, (waitNanos + 999_999_999L) / 1_000_000_000L);
            return new RateLimitDecision(false, retryAfterSeconds);
        }

        requestTimes.addLast(now);
        if (++acceptedRequests % CLEANUP_EVERY == 0) {
            removeExpiredIpBuckets(now);
        }
        return new RateLimitDecision(true, 0);
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

    private record RateLimitDecision(boolean allowed, long retryAfterSeconds) {
    }
}
