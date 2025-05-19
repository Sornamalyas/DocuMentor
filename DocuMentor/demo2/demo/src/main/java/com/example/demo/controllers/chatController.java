package com.example.demo.controllers;

import com.example.demo.chatbot.Chunker;
import com.example.demo.chatbot.QABot;
import com.example.demo.model.User;
import com.example.demo.services.EmbeddingStoreService;
import com.example.demo.services.UserService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class chatController {
    @Autowired
    private UserService userService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private EmbeddingStoreService embeddingStoreService;
    private static final Logger logger = LoggerFactory.getLogger(chatController.class);
    @GetMapping("/chat")
    public String home(Model model, @AuthenticationPrincipal OAuth2User user) {
        if (user == null) {
            logger.warn("Unauthenticated access attempt to chat");
            return "redirect:/"; // Redirect to login if user is not authenticated
        }
        String userName = user.getAttribute("name");
        String email = user.getAttribute("email");

        // Check if user exists in database, create if not
        User existingUser = userService.getUserByEmail(email);
        if (existingUser == null) {
            logger.info("Creating new user: {}", email);
            User newUser = new User(userName, email);
            userService.createUser(newUser);
        }
        model.addAttribute("userName", userName);
        return "chat";
    }
    @RateLimiter(name = "apiRateLimiter", fallbackMethod = "fallbackMethod")
    @PostMapping("/chat")
    public String handleFileUpload(@RequestParam(value = "file", required = true) MultipartFile file,
                                   Model model,
                                   @AuthenticationPrincipal OAuth2User user) throws IOException, TikaException {
        if (user == null) {
            logger.warn("Unauthenticated user attempted to upload a file");
            return "redirect:/"; // Ensure user is authenticated
        }
        String userName = user.getAttribute("name");
        String email = user.getAttribute("email");
        logger.info("User {} is uploading file: {}", email, file.getOriginalFilename());
        model.addAttribute("userName", userName);
        if (file == null || file.isEmpty()) {
            logger.warn("Empty file upload by user: {}", email);
            model.addAttribute("message", "Please upload a file.");
            return "chat";
        }
        // Validate file type and size
        if (!isAllowedFileType(file.getContentType(), file.getOriginalFilename())) {
            logger.warn("Invalid file type uploaded by {}: {}", email, file.getContentType());
            model.addAttribute("message", "Invalid file type. Only PDF, DOCX, and TXT are allowed.");
            return "chat";
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            logger.warn("File too large: {} bytes from user {}", file.getSize(), email);
            model.addAttribute("message", "File size exceeds the 5MB limit.");
            return "chat";
        }
        // Process the file
        String originalFilename = sanitizeFilename(file);
        if (originalFilename.contains("..")){
            logger.error("Directory traversal attempt in filename: {}", originalFilename);
            return "Invalid filename detected";
        }
        User existingUser = userService.getUserByEmail(email);
        if (!userService.fileExistsForUser(existingUser, file.getOriginalFilename())) {
            // System.out.println(existingUser.getDocument().getDocName()+"   "+ file.getOriginalFilename());
            logger.info("Processing new file: {}", originalFilename);
            mongoTemplate.remove(new Query(), existingUser.getName());
            logger.info("Old documents removed for user: {}", email);
            logger.info("Uploading document for user: {}", email);
            System.out.println(userService.uploadDocument(file, email));
            String extractedText = extractText(file);
            Chunker.storeChunks(extractedText, userName);
            model.addAttribute("message", "File processed successfully.");
        } else {
            logger.info("File already exists for user {}: {}", email, file.getOriginalFilename());
            model.addAttribute("message", "File already exists. No need for reprocessing.");
            //useLastFile(user);
        }
        return "chat";
    }
    @RateLimiter(name = "apiRateLimiter", fallbackMethod = "fallbackMethod")
    @PostMapping("/ask")
    @ResponseBody
    public String handleQuestion(@RequestParam String question,
                                 @AuthenticationPrincipal OAuth2User user) {
        if(question.length()>=200){
            return "Query is too long";
        }

        if(question.matches(".*(script|<|>|onerror|eval|iframe|javascript:).*")){
            return "Invalid prompt";                      //prompt injection
        }
        if (user == null) return "Unauthorized";

        question=StringEscapeUtils.escapeHtml4(question);
        String email = user.getAttribute("email");
        User existingUser = userService.getUserByEmail(email);
        if (existingUser == null) return "User not found.";
        return QABot.getAnswer(existingUser, question);
    }
    @RateLimiter(name = "apiRateLimiter", fallbackMethod = "fallbackMethod")
    @PostMapping("/useLast")
    @ResponseBody
    public String useLastFile(@AuthenticationPrincipal OAuth2User user) {
        if (user == null) return "Unauthorized";
        String email = user.getAttribute("email");
        User existingUser = userService.getUserByEmail(email);
        String file = existingUser.getDocument().getDocName();
        if (file == null || file.isEmpty()) {
            return "No document present";
        }
        return file;                  // Return just the file name
    }
    private boolean isAllowedFileType(String contentType, String filename) {
        if (contentType == null || filename == null) return false;
        String lowerFilename = filename.toLowerCase();
        return (contentType.equals("application/pdf") && lowerFilename.endsWith(".pdf")) ||
                (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") && lowerFilename.endsWith(".docx")) ||
                (contentType.equals("application/msword") && lowerFilename.endsWith(".doc")) ||
                (contentType.equals("text/plain") && lowerFilename.endsWith(".txt"));
    }
    private String sanitizeFilename(MultipartFile filename) {
        String file_name = StringUtils.cleanPath(filename.getOriginalFilename());
        return FilenameUtils.getName(file_name);
    }
    private String extractText(MultipartFile file) throws IOException, TikaException {
        Tika tika = new Tika();
        return StringEscapeUtils.escapeHtml4(tika.parseToString(file.getInputStream()));
    }
    public String fallbackMethod(String message, Exception e) {
        return "Too many requests. Please try again later.";
    }
}


