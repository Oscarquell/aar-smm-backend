package kg.azem.portfolio.admin;

import java.util.List;
import kg.azem.portfolio.portfolio.PortfolioItem;
import kg.azem.portfolio.portfolio.PortfolioMedia;
import kg.azem.portfolio.portfolio.PortfolioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminPortfolioService {
    private final PortfolioRepository repository;
    private final MediaStorageService mediaStorage;

    public AdminPortfolioService(PortfolioRepository repository, MediaStorageService mediaStorage) {
        this.repository = repository;
        this.mediaStorage = mediaStorage;
    }

    public List<PortfolioItem> getAll() {
        return repository.findAllForAdmin();
    }

    public PortfolioItem get(long id) {
        return repository.findForAdmin(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Работа не найдена"));
    }

    public PortfolioItem create(AdminPortfolioRequest request) {
        validateHomeFeature(request, 0);
        try {
            return get(repository.create(request));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Работа с таким slug уже существует");
        }
    }

    public PortfolioItem update(long id, AdminPortfolioRequest request) {
        get(id);
        validateHomeFeature(request, id);
        try {
            if (!repository.update(id, request)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Работа не найдена");
            }
            return get(id);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Работа с таким slug уже существует");
        }
    }

    private void validateHomeFeature(AdminPortfolioRequest request, long id) {
        if (request.featuredOnHome() && !"PROJECT".equals(request.type())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "На главной можно выделять только проекты");
        }
        if (request.featuredOnHome() && repository.countFeaturedOnHomeExcept(id) >= 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "На главной можно выделить не более двух работ");
        }
    }

    public void delete(long id) {
        PortfolioItem item = get(id);
        item.media().forEach(media -> mediaStorage.deleteIfManaged(media.url()));
        repository.deleteItem(id);
    }

    public PortfolioMedia uploadMedia(long itemId, MultipartFile file, String altText, String posterUrl, int sortOrder) {
        get(itemId);
        MediaStorageService.StoredMedia stored = mediaStorage.store(file);
        try {
            return repository.insertMedia(
                    itemId,
                    stored.type(),
                    stored.url(),
                    altText == null ? "" : altText.trim(),
                    posterUrl == null || posterUrl.isBlank() ? null : posterUrl.trim(),
                    sortOrder
            );
        } catch (RuntimeException exception) {
            mediaStorage.deleteIfManaged(stored.url());
            throw exception;
        }
    }

    public PortfolioMedia updateMedia(long itemId, long mediaId, AdminMediaUpdateRequest request) {
        PortfolioMedia media = repository.findMediaForItem(itemId, mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Медиафайл не найден"));
        repository.updateMedia(itemId, mediaId, request);
        return repository.findMediaForItem(itemId, mediaId).orElse(media);
    }

    public void deleteMedia(long itemId, long mediaId) {
        String url = repository.deleteMedia(itemId, mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Медиафайл не найден"));
        mediaStorage.deleteIfManaged(url);
    }
}
