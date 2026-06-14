package com.mindpulse.backend.controller;

import com.mindpulse.backend.annotation.AuditLogAnnotation;
import com.mindpulse.backend.dto.ApiResponse;
import com.mindpulse.backend.dto.NoteDto;
import com.mindpulse.backend.entity.Note;
import com.mindpulse.backend.exception.ResourceNotFoundException;
import com.mindpulse.backend.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notes")
@Tag(name = "Note Management", description = "Note CRUD and async AI summary generation interface")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationServiceException("User is not authenticated");
        }
        return authentication.getName();
    }

    @Operation(summary = "Sync upload note", description = "Upload note content and optional attachment, save synchronously to database")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Note created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Creation failed")
    })
    @AuditLogAnnotation(action = "CREATE", resourceType = "NOTE")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> createNote(
            @Parameter(description = "Note title") @RequestParam("title") String title,
            @Parameter(description = "Note content") @RequestParam("content") String content,
            @Parameter(description = "Tags, comma-separated") @RequestParam(value = "tags", required = false) String tags,
            @Parameter(description = "Attachment file") @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        String username = getCurrentUsername();
        Note createdNote = noteService.uploadNote(title, content, username, tags, file);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Note created successfully");
        data.put("note", createdNote);
        return ResponseEntity.status(201).body(ApiResponse.success(201, "Note created successfully", data));
    }

    @Operation(summary = "Async upload note with summary", description = "Upload note content and return immediately, AI generates summary and tags asynchronously, pushes result via WebSocket on completion")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Note submitted, processing asynchronously",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Submission failed")
    })
    @PostMapping(value = "/async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> createNoteAsync(
            @Parameter(description = "Note title", required = true) @RequestParam("title") String title,
            @Parameter(description = "Note content", required = true) @RequestParam("content") String content,
            @Parameter(description = "Tags, comma-separated") @RequestParam(value = "tags", required = false) String tags,
            @Parameter(description = "Attachment file") @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        String username = getCurrentUsername();
        Map<String, Object> result = noteService.createNoteAsync(title, content, username, tags, file);

        return ResponseEntity.status(201).body(
                ApiResponse.success(201, "Note submitted, summary processing asynchronously", result));
    }

    @Operation(summary = "Get all notes", description = "Get all notes for the current user, supports keyword search")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Note>>> getAllNotes(
            @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword) {
        String username = getCurrentUsername();

        List<Note> notes;
        if (keyword != null && !keyword.isEmpty()) {
            notes = noteService.searchNotes(keyword, username);
        } else {
            notes = noteService.getAllNotesByUser(username);
        }

        return ResponseEntity.ok(ApiResponse.success(notes));
    }

    @Operation(summary = "Get note by ID", description = "Get single note details, ownership verification required")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Note>> getNoteById(
            @Parameter(description = "Note ID") @PathVariable Long id) {
        Note note = noteService.getNoteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));

        String username = getCurrentUsername();
        noteService.verifyOwnership(note.getAuthor(), username, id);

        return ResponseEntity.ok(ApiResponse.success(note));
    }

    @Operation(summary = "Update note", description = "Update note title, content, tags, etc. Ownership verification required")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Note>> updateNote(
            @Parameter(description = "Note ID") @PathVariable Long id,
            @Valid @RequestBody NoteDto noteDto) {
        Note existingNote = noteService.getNoteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));

        String username = getCurrentUsername();
        noteService.verifyOwnership(existingNote.getAuthor(), username, id);

        NoteDto updatedNoteDto = new NoteDto(
            noteDto.id(),
            noteDto.title(),
            noteDto.content(),
            noteDto.type(),
            noteDto.fileUrl(),
            noteDto.tags(),
            noteDto.summary(),
            noteDto.category(),
            noteDto.status(),
            username
        );
        Note updatedNote = noteService.updateNote(id, updatedNoteDto);
        return ResponseEntity.ok(ApiResponse.success("Note updated successfully", updatedNote));
    }

    @Operation(summary = "Delete note", description = "Delete note by ID, ownership verification required")
    @AuditLogAnnotation(action = "DELETE", resourceType = "NOTE")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteNote(
            @Parameter(description = "Note ID") @PathVariable Long id) {
        Note existingNote = noteService.getNoteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));

        String username = getCurrentUsername();
        noteService.verifyOwnership(existingNote.getAuthor(), username, id);

        noteService.deleteNote(id);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Note deleted successfully");
        return ResponseEntity.ok(ApiResponse.success("Note deleted successfully", data));
    }

    @Operation(summary = "Download note file", description = "Download the attached file of a note")
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Note ID") @PathVariable Long id) {
        String username = getCurrentUsername();
        Note note = noteService.getNoteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));

        noteService.verifyOwnership(note.getAuthor(), username, id);

        if (note.getFileUrl() == null || note.getFileUrl().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        java.io.File file = new java.io.File(note.getFileUrl());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    @Operation(summary = "Preview note file inline", description = "Serve file with correct MIME type for inline preview in browser")
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previewFile(
            @Parameter(description = "Note ID") @PathVariable Long id,
            @Parameter(description = "JWT token for authentication") @RequestParam(required = false) String token) {
        String username = getCurrentUsername();
        Note note = noteService.getNoteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));

        noteService.verifyOwnership(note.getAuthor(), username, id);

        if (note.getFileUrl() == null || note.getFileUrl().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        java.io.File file = new java.io.File(note.getFileUrl());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String contentType = getContentType(file.getName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    private String getContentType(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) return "application/octet-stream";
        String ext = filename.substring(dotIndex + 1).toLowerCase();
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            case "txt", "csv", "json", "xml", "log" -> "text/plain";
            case "md" -> "text/markdown";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            default -> "application/octet-stream";
        };
    }

    @Operation(summary = "Generate summary synchronously (deprecated)", description = "This endpoint is for debugging and backward compatibility only. Use POST /api/notes/async instead.")
    @PostMapping("/{id}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateSummary(
            @Parameter(description = "Note ID") @PathVariable Long id) {
        Note note = noteService.getNoteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));

        String username = getCurrentUsername();
        noteService.verifyOwnership(note.getAuthor(), username, id);

        Map<String, Object> summaryResult = noteService.generateSummary(id, username);

        return ResponseEntity.ok(ApiResponse.success("Summary generated and note updated successfully", summaryResult));
    }
}
