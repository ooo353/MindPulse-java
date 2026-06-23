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
@Tag(name = "笔记管理", description = "笔记增删改查及异步AI摘要生成接口")
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

    @Operation(summary = "同步上传笔记", description = "上传笔记内容和可选附件，同步保存到数据库")
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
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("Title is required"));
        }
        String username = getCurrentUsername();
        Note createdNote = noteService.uploadNote(title, content, username, tags, file);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "Note created successfully");
        data.put("note", createdNote);
        return ResponseEntity.status(201).body(ApiResponse.success(201, "Note created successfully", data));
    }

    @Operation(summary = "异步上传笔记并生成摘要", description = "上传笔记内容后立即返回，AI异步生成摘要和标签，完成后通过WebSocket推送结果")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Note submitted, processing asynchronously",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Submission failed")
    })
    @AuditLogAnnotation(action = "CREATE", resourceType = "NOTE")
    @PostMapping(value = "/async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> createNoteAsync(
            @Parameter(description = "Note title", required = true) @RequestParam("title") String title,
            @Parameter(description = "Note content", required = true) @RequestParam("content") String content,
            @Parameter(description = "Tags, comma-separated") @RequestParam(value = "tags", required = false) String tags,
            @Parameter(description = "Attachment file") @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("Title is required"));
        }
        String username = getCurrentUsername();
        Map<String, Object> result = noteService.createNoteAsync(title, content, username, tags, file);

        return ResponseEntity.status(201).body(
                ApiResponse.success(201, "Note submitted, summary processing asynchronously", result));
    }

    @Operation(summary = "获取所有笔记", description = "获取当前用户的所有笔记，支持关键词搜索")
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

    @Operation(summary = "获取笔记详情", description = "获取单个笔记详情，需要所有权验证")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Note>> getNoteById(
            @Parameter(description = "Note ID") @PathVariable Long id) {
        Note note = noteService.getNoteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));

        String username = getCurrentUsername();
        noteService.verifyOwnership(note.getAuthor(), username, id);

        return ResponseEntity.ok(ApiResponse.success(note));
    }

    @Operation(summary = "更新笔记", description = "更新笔记标题、内容、标签等，需要所有权验证")
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

    @Operation(summary = "删除笔记", description = "根据ID删除笔记，需要所有权验证")
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

    @Operation(summary = "下载笔记附件", description = "下载笔记的附件文件")
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> downloadFile(
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
        String originalFilename = extractOriginalFilename(file.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFilename + "\"")
                .body(resource);
    }

    @Operation(summary = "预览笔记附件", description = "以正确的MIME类型返回文件，支持浏览器内联预览")
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
        String originalFilename = extractOriginalFilename(file.getName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + originalFilename + "\"")
                .body(resource);
    }

    /**
     * Extract original filename from UUID-prefixed stored filename.
     * Stored format: {uuid}_{originalName} → returns {originalName}
     */
    private String extractOriginalFilename(String storedName) {
        if (storedName == null) return "download";
        int underscoreIdx = storedName.indexOf('_');
        if (underscoreIdx > 0 && underscoreIdx < storedName.length() - 1) {
            // Verify the prefix looks like a UUID (contains hyphens)
            String prefix = storedName.substring(0, underscoreIdx);
            if (prefix.contains("-")) {
                return storedName.substring(underscoreIdx + 1);
            }
        }
        return storedName;
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

    @Operation(summary = "同步生成摘要（已废弃）", description = "此接口仅用于调试和向后兼容，请使用 POST /api/notes/async 代替")
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
