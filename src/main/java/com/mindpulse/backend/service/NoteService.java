package com.mindpulse.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindpulse.backend.dto.NoteDto;
import com.mindpulse.backend.dto.NoteSummaryMessage;
import com.mindpulse.backend.entity.Note;
import com.mindpulse.backend.exception.ResourceNotFoundException;
import com.mindpulse.backend.mapper.NoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService implements INoteService {

    private final NoteMapper noteMapper;
    private final NoteSummaryProducer noteSummaryProducer;
    private final AiAgentClient aiAgentClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${mindpulse.upload.path:./uploads}")
    private String uploadPath;

    private static final String CACHE_PREFIX = "notes:";
    private static final long CACHE_TTL_MINUTES = 10;

    // --- Manual cache helpers ---

    private <T> T getFromCache(String key, TypeReference<T> typeRef) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, typeRef);
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed for key={}: {}", key, e.getMessage());
        }
        return null;
    }

    private void putToCache(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis cache write failed for key={}: {}", key, e.getMessage());
        }
    }

    private void evictCache(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis cache evict failed for key={}: {}", key, e.getMessage());
        }
    }

    private void evictCacheByPattern(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis cache evict failed for pattern={}: {}", pattern, e.getMessage());
        }
    }

    // --- Read operations ---

    @Override
    public List<Note> getAllNotesByUser(String username) {
        String key = CACHE_PREFIX + "author_" + username;
        List<Note> cached = getFromCache(key, new TypeReference<List<Note>>() {});
        if (cached != null) {
            return cached;
        }
        List<Note> notes = noteMapper.findByAuthor(username);
        putToCache(key, notes);
        return notes;
    }

    @Override
    public List<Note> searchNotes(String keyword, String username) {
        String key = CACHE_PREFIX + "search_" + keyword + "_" + username;
        List<Note> cached = getFromCache(key, new TypeReference<List<Note>>() {});
        if (cached != null) {
            return cached;
        }
        List<Note> notes = noteMapper.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(keyword, username);
        putToCache(key, notes);
        return notes;
    }

    @Override
    public Optional<Note> getNoteById(Long id) {
        String key = CACHE_PREFIX + "id_" + id;
        Note cached = getFromCache(key, new TypeReference<Note>() {});
        if (cached != null) {
            return Optional.of(cached);
        }
        Note note = noteMapper.findById(id);
        if (note != null) {
            putToCache(key, note);
            return Optional.of(note);
        }
        return Optional.empty();
    }

    // --- Write operations ---

    @Override
    @Transactional
    public Note createNote(NoteDto noteDto) {
        Note note = new Note();
        note.setTitle(noteDto.title());
        note.setContent(noteDto.content());
        note.setType(noteDto.type());
        note.setTags(noteDto.tags());
        note.setSummary(noteDto.summary());
        note.setCategory(noteDto.category());
        note.setStatus(noteDto.status() != null ? noteDto.status() : "processing");
        note.setAuthor(noteDto.author());
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        noteMapper.insertNote(note);
        evictCacheByPattern(CACHE_PREFIX + "*");
        return note;
    }

    @Override
    @Transactional
    public Note uploadNote(String title, String content, String author, String tags, MultipartFile file) throws IOException {
        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setAuthor(author);
        note.setTags(tags);
        note.setStatus("processing");
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new RuntimeException("File size exceeds maximum limit of 10MB");
            }
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String filePath = uploadPath + File.separator + fileName;

            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(file.getBytes());
            }

            note.setFileUrl(filePath);
            note.setType(determineFileType(file.getOriginalFilename()));
        }

        noteMapper.insertNote(note);
        evictCacheByPattern(CACHE_PREFIX + "*");
        return note;
    }

    @Override
    @Transactional
    public Map<String, Object> createNoteAsync(String title, String content, String author,
                                                String tags, MultipartFile file) throws IOException {
        Note note = uploadNote(title, content, author, tags, file);
        log.info("Note saved: noteId={}, submitting summary task to message queue", note.getId());

        NoteSummaryMessage message = new NoteSummaryMessage(
                note.getId(), note.getContent(), note.getTitle(), author, note.getFileUrl()
        );
        String noteStatus = "processing";
        try {
            noteSummaryProducer.sendSummaryTask(message);
        } catch (Exception e) {
            log.error("Failed to submit summary task for note {}: {}", note.getId(), e.getMessage());
            noteStatus = "failed";
            note.setStatus("failed");
            noteMapper.updateNote(note);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("noteId", note.getId());
        result.put("status", noteStatus);
        result.put("message", noteStatus.equals("processing")
                ? "Note submitted, summary processing asynchronously"
                : "Note saved, but summary processing unavailable (RabbitMQ not running)");
        result.put("note", note);
        return result;
    }

    @Override
    @Transactional
    public Note updateNote(Long id, NoteDto noteDto) {
        Note existingNote = noteMapper.findById(id);

        if (existingNote != null) {
            existingNote.setTitle(noteDto.title());
            existingNote.setContent(noteDto.content());
            existingNote.setType(noteDto.type());
            existingNote.setTags(noteDto.tags());
            existingNote.setSummary(noteDto.summary());
            existingNote.setCategory(noteDto.category());
            if (noteDto.status() != null) {
                existingNote.setStatus(noteDto.status());
            }
            existingNote.setUpdatedAt(LocalDateTime.now());

            noteMapper.updateNote(existingNote);
            evictCache(CACHE_PREFIX + "id_" + id);
            evictCacheByPattern(CACHE_PREFIX + "author_*");
            return existingNote;
        } else {
            throw new RuntimeException("Note not found with id: " + id);
        }
    }

    @Override
    @Transactional
    public void deleteNote(Long id) {
        noteMapper.deleteById(id);
        evictCache(CACHE_PREFIX + "id_" + id);
        evictCacheByPattern(CACHE_PREFIX + "*");
    }

    /**
     * Evict Redis cache for a specific note and all author lists.
     * Called by NoteSummaryConsumer after async summary update.
     */
    public void evictNoteCache(Long noteId) {
        evictCache(CACHE_PREFIX + "id_" + noteId);
        evictCacheByPattern(CACHE_PREFIX + "author_*");
        evictCacheByPattern(CACHE_PREFIX + "search_*");
    }

    @Override
    @Transactional
    public Map<String, Object> generateSummary(Long noteId, String username) {
        Note note = noteMapper.findById(noteId);
        if (note == null) {
            throw new ResourceNotFoundException("Note not found with id: " + noteId);
        }
        verifyOwnership(note.getAuthor(), username, noteId);

        Map<String, Object> summaryResult = aiAgentClient.generateSummary(note.getContent());

        String newTitle = (String) summaryResult.get("title");
        if (newTitle != null && !newTitle.isEmpty() && !newTitle.equals("Auto-generated Title")) {
            note.setTitle(newTitle);
        }

        String newTags = (String) summaryResult.get("tags");
        if (newTags != null && !newTags.isEmpty()) {
            if (note.getTags() != null && !note.getTags().isEmpty()) {
                note.setTags(note.getTags() + "," + newTags);
            } else {
                note.setTags(newTags);
            }
        }

        note = updateNote(noteId,
                new NoteDto(note.getId(), note.getTitle(), note.getContent(),
                        note.getType(), note.getFileUrl(), note.getTags(),
                        note.getSummary(), note.getCategory(), note.getStatus(),
                        note.getAuthor()));

        Map<String, Object> data = new HashMap<>();
        data.put("summary", summaryResult.get("summary"));
        data.put("tags", summaryResult.get("tags"));
        data.put("updated_note", note);
        data.put("message", "Summary generated and note updated successfully");
        return data;
    }

    @Override
    public void verifyOwnership(String entityAuthor, String currentUser, Long entityId) {
        if (!entityAuthor.equals(currentUser)) {
            throw new AccessDeniedException("You do not have permission to access entity with id " + entityId);
        }
    }

    private String determineFileType(String fileName) {
        if (fileName == null) return "text";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) return "text";
        String extension = fileName.substring(lastDot + 1).toLowerCase();
        switch (extension) {
            case "pdf": return "pdf";
            case "jpg": case "jpeg": case "png": case "gif": case "bmp": case "webp": return "image";
            case "md": return "markdown";
            case "doc": case "docx": return "document";
            case "xls": case "xlsx": return "spreadsheet";
            case "ppt": case "pptx": return "presentation";
            case "txt": case "csv": case "json": case "xml": return "text";
            default: return "file";
        }
    }
}
