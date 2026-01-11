package com.petruth.urlshortener;

import com.petruth.urlshortener.dto.LinkSearchRequest;
import com.petruth.urlshortener.entity.ShortenedUrl;
import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.repository.ShortenedUrlRepository;
import com.petruth.urlshortener.service.ShortenedUrlServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortenedUrlServiceImplTest {

    @Mock
    private ShortenedUrlRepository repository;

    @InjectMocks
    private ShortenedUrlServiceImpl service;

    private User testUser;
    private ShortenedUrl testUrl;
    private ShortenedUrl testUrl2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        testUrl = new ShortenedUrl();
        testUrl.setId(1L);
        testUrl.setCode("abc123");
        testUrl.setLongUrl("https://example.com");
        testUrl.setShortUrl("http://localhost/api/abc123");
        testUrl.setUser(testUser);
        testUrl.setClickCount(10L);
        testUrl.setDateCreated(LocalDateTime.now());

        testUrl2 = new ShortenedUrl();
        testUrl2.setId(2L);
        testUrl2.setCode("xyz789");
        testUrl2.setLongUrl("https://test.com");
        testUrl2.setShortUrl("http://localhost/api/xyz789");
        testUrl2.setUser(testUser);
        testUrl2.setClickCount(5L);
        testUrl2.setDateCreated(LocalDateTime.now());
        testUrl2.setExpiresAt(LocalDateTime.now().plusDays(7));

        pageable = PageRequest.of(0, 10);
    }

    // ===== EXISTING METHODS TESTS =====

    @Test
    void generateUniqueCode_ShouldReturnUniqueCode() {
        // Given
        when(repository.existsByCode(anyString())).thenReturn(false);

        // When
        String code = service.generateUniqueCode();

        // Then
        assertNotNull(code);
        assertEquals(7, code.length());
        assertTrue(code.matches("[A-Za-z0-9]+"));
        verify(repository, atLeastOnce()).existsByCode(anyString());
    }

    @Test
    void generateUniqueCode_ShouldRetryIfCodeExists() {
        // Given
        when(repository.existsByCode(anyString()))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        // When
        String code = service.generateUniqueCode();

        // Then
        assertNotNull(code);
        verify(repository, times(3)).existsByCode(anyString());
    }

    @Test
    void save_ShouldSaveAndReturnUrl() {
        // Given
        when(repository.save(any(ShortenedUrl.class))).thenReturn(testUrl);

        // When
        ShortenedUrl result = service.save(testUrl);

        // Then
        assertNotNull(result);
        assertEquals(testUrl.getCode(), result.getCode());
        verify(repository, times(1)).save(testUrl);
    }

    @Test
    void findByCode_ShouldReturnUrl_WhenExists() {
        // Given
        when(repository.findByCode("abc123")).thenReturn(Optional.of(testUrl));

        // When
        ShortenedUrl result = service.findByCode("abc123");

        // Then
        assertNotNull(result);
        assertEquals("abc123", result.getCode());
        verify(repository, times(1)).findByCode("abc123");
    }

    @Test
    void findByCode_ShouldThrowException_WhenNotFound() {
        // Given
        when(repository.findByCode("xyz")).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> service.findByCode("xyz"));
        verify(repository, times(1)).findByCode("xyz");
    }

    @Test
    void existsByCode_ShouldReturnTrue_WhenCodeExists() {
        // Given
        when(repository.existsByCode("abc123")).thenReturn(true);

        // When
        boolean exists = service.existsByCode("abc123");

        // Then
        assertTrue(exists);
        verify(repository, times(1)).existsByCode("abc123");
    }

    @Test
    void existsByCode_ShouldReturnFalse_WhenCodeDoesNotExist() {
        // Given
        when(repository.existsByCode("xyz")).thenReturn(false);

        // When
        boolean exists = service.existsByCode("xyz");

        // Then
        assertFalse(exists);
        verify(repository, times(1)).existsByCode("xyz");
    }

    @Test
    void delete_ShouldCallRepositoryDelete() {
        // Given
        doNothing().when(repository).delete(testUrl);

        // When
        service.delete(testUrl);

        // Then
        verify(repository, times(1)).delete(testUrl);
    }

    @Test
    void findByUser_ShouldReturnListOfUrls() {
        // Given
        List<ShortenedUrl> urls = Arrays.asList(testUrl, testUrl2);
        when(repository.findByUser(testUser)).thenReturn(Optional.of(urls));

        // When
        List<ShortenedUrl> result = service.findByUser(testUser);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findByUser(testUser);
    }

    // ===== NEW PAGINATION & SEARCH METHODS TESTS =====

    @Test
    void findByUserPaginated_ShouldReturnPagedResults() {
        // Given
        List<ShortenedUrl> urls = Arrays.asList(testUrl, testUrl2);
        Page<ShortenedUrl> page = new PageImpl<>(urls, pageable, urls.size());

        when(repository.findByUser(testUser, pageable)).thenReturn(page);

        // When
        Page<ShortenedUrl> result = service.findByUserPaginated(testUser, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(testUrl.getCode(), result.getContent().get(0).getCode());
        verify(repository, times(1)).findByUser(testUser, pageable);
    }

    @Test
    void searchLinks_WithEmptySearchTerm_ShouldReturnAllUserLinks() {
        // Given
        List<ShortenedUrl> urls = Arrays.asList(testUrl, testUrl2);
        Page<ShortenedUrl> page = new PageImpl<>(urls, pageable, urls.size());

        when(repository.findByUser(testUser, pageable)).thenReturn(page);

        // When
        Page<ShortenedUrl> result = service.searchLinks(testUser, "", pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(repository, times(1)).findByUser(testUser, pageable);
        verify(repository, never()).searchByTerm(any(), anyString(), any());
    }

    @Test
    void searchLinks_WithSearchTerm_ShouldCallSearchByTerm() {
        // Given
        String searchTerm = "example";
        List<ShortenedUrl> urls = Arrays.asList(testUrl);
        Page<ShortenedUrl> page = new PageImpl<>(urls, pageable, urls.size());

        when(repository.searchByTerm(testUser, searchTerm, pageable)).thenReturn(page);

        // When
        Page<ShortenedUrl> result = service.searchLinks(testUser, searchTerm, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("abc123", result.getContent().get(0).getCode());
        verify(repository, times(1)).searchByTerm(testUser, searchTerm, pageable);
        verify(repository, never()).findByUser(any(User.class), any(Pageable.class));
    }

    @Test
    void searchLinks_WithNullSearchTerm_ShouldReturnAllUserLinks() {
        // Given
        List<ShortenedUrl> urls = Arrays.asList(testUrl, testUrl2);
        Page<ShortenedUrl> page = new PageImpl<>(urls, pageable, urls.size());

        when(repository.findByUser(testUser, pageable)).thenReturn(page);

        // When
        Page<ShortenedUrl> result = service.searchLinks(testUser, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(repository, times(1)).findByUser(testUser, pageable);
    }

    @Test
    void advancedSearchLinks_ShouldCallAdvancedSearch() {
        // Given
        LinkSearchRequest request = new LinkSearchRequest(
                "example",
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now(),
                5L,
                100L,
                null,
                "dateCreated",
                "desc"
        );

        List<ShortenedUrl> urls = Arrays.asList(testUrl);
        Page<ShortenedUrl> page = new PageImpl<>(urls, pageable, urls.size());

        when(repository.advancedSearch(
                eq(testUser),
                eq(request.searchTerm()),
                eq(request.startDate()),
                eq(request.endDate()),
                eq(request.minClicks()),
                eq(request.maxClicks()),
                eq(pageable)
        )).thenReturn(page);

        // When
        Page<ShortenedUrl> result = service.advancedSearchLinks(testUser, request, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(repository, times(1)).advancedSearch(
                testUser,
                request.searchTerm(),
                request.startDate(),
                request.endDate(),
                request.minClicks(),
                request.maxClicks(),
                pageable
        );
    }

    @Test
    void findExpiredLinks_ShouldReturnOnlyExpiredLinks() {
        // Given
        ShortenedUrl expiredUrl = new ShortenedUrl();
        expiredUrl.setId(3L);
        expiredUrl.setCode("exp123");
        expiredUrl.setExpiresAt(LocalDateTime.now().minusDays(1));
        expiredUrl.setUser(testUser);

        List<ShortenedUrl> urls = Arrays.asList(expiredUrl);
        Page<ShortenedUrl> page = new PageImpl<>(urls, pageable, urls.size());

        when(repository.findByUserAndExpiresAtBefore(eq(testUser), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(page);

        // When
        Page<ShortenedUrl> result = service.findExpiredLinks(testUser, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("exp123", result.getContent().get(0).getCode());
        verify(repository, times(1)).findByUserAndExpiresAtBefore(
                eq(testUser), any(LocalDateTime.class), eq(pageable));
    }

    @Test
    void findActiveLinks_ShouldReturnNonExpiredLinks() {
        // Given
        LinkSearchRequest request = new LinkSearchRequest(
                null, null, null, null, null, false, "dateCreated", "desc"
        );

        List<ShortenedUrl> urls = Arrays.asList(testUrl, testUrl2);
        Page<ShortenedUrl> page = new PageImpl<>(urls, pageable, urls.size());

        when(repository.advancedSearch(
                eq(testUser),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(pageable)
        )).thenReturn(page);

        // When
        Page<ShortenedUrl> result = service.findActiveLinks(testUser, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(repository, times(1)).advancedSearch(
                eq(testUser), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void countUserLinks_ShouldReturnCorrectCount() {
        // Given
        when(repository.countByUser(testUser)).thenReturn(5L);

        // When
        long count = service.countUserLinks(testUser);

        // Then
        assertEquals(5, count);
        verify(repository, times(1)).countByUser(testUser);
    }

    @Test
    void countExpiredLinks_ShouldReturnCorrectCount() {
        // Given
        when(repository.countByUserAndExpiresAtBefore(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(3L);

        // When
        long count = service.countExpiredLinks(testUser);

        // Then
        assertEquals(3, count);
        verify(repository, times(1)).countByUserAndExpiresAtBefore(
                eq(testUser), any(LocalDateTime.class));
    }

    @Test
    void countActiveLinks_ShouldReturnCorrectCount() {
        // Given
        when(repository.countByUserAndExpiresAtAfter(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(5L);
        when(repository.countByUserAndExpiresAtIsNull(testUser)).thenReturn(2L);

        // When
        long count = service.countActiveLinks(testUser);

        // Then
        assertEquals(7, count); // 5 + 2
        verify(repository, times(1)).countByUserAndExpiresAtAfter(
                eq(testUser), any(LocalDateTime.class));
        verify(repository, times(1)).countByUserAndExpiresAtIsNull(testUser);
    }

    @Test
    void countActiveLinks_ShouldHandleZeroCounts() {
        // Given
        when(repository.countByUserAndExpiresAtAfter(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(repository.countByUserAndExpiresAtIsNull(testUser)).thenReturn(0L);

        // When
        long count = service.countActiveLinks(testUser);

        // Then
        assertEquals(0, count);
    }

    // ===== EDGE CASES & ERROR HANDLING =====

    @Test
    void findByUserPaginated_WithEmptyResult_ShouldReturnEmptyPage() {
        // Given
        Page<ShortenedUrl> emptyPage = new PageImpl<>(Arrays.asList(), pageable, 0);
        when(repository.findByUser(testUser, pageable)).thenReturn(emptyPage);

        // When
        Page<ShortenedUrl> result = service.findByUserPaginated(testUser, pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void searchLinks_WithWhitespaceSearchTerm_ShouldReturnAllLinks() {
        // Given
        List<ShortenedUrl> urls = Arrays.asList(testUrl, testUrl2);
        Page<ShortenedUrl> page = new PageImpl<>(urls, pageable, urls.size());

        when(repository.findByUser(testUser, pageable)).thenReturn(page);

        // When
        Page<ShortenedUrl> result = service.searchLinks(testUser, "   ", pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(repository, times(1)).findByUser(testUser, pageable);
    }

    @Test
    void generateUniqueCode_ShouldGenerateValidCharacters() {
        // Given
        when(repository.existsByCode(anyString())).thenReturn(false);

        // When
        String code = service.generateUniqueCode();

        // Then
        // Should only contain alphanumeric characters
        assertTrue(code.matches("^[A-Za-z0-9]+$"));
        // Should be exactly 7 characters
        assertEquals(7, code.length());
    }
}