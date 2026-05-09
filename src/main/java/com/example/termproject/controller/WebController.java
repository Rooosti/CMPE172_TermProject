package com.example.termproject.controller;

import com.example.termproject.model.Booking;
import com.example.termproject.model.ChatMessage;
import com.example.termproject.model.User;
import com.example.termproject.repository.BookingRepository;
import com.example.termproject.repository.EquipmentRepository;
import com.example.termproject.repository.MessageRepository;
import com.example.termproject.repository.UserRepository;
import com.example.termproject.service.BookingService;
import com.example.termproject.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Controller
public class WebController {

    private final EquipmentRepository equipmentRepository;
    private final BookingService bookingService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final BookingRepository bookingRepository;

    public WebController(EquipmentRepository equipmentRepository, 
                         BookingService bookingService, 
                         UserService userService,
                         UserRepository userRepository,
                         MessageRepository messageRepository,
                         BookingRepository bookingRepository) {
        this.equipmentRepository = equipmentRepository;
        this.bookingService = bookingService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String firstName,
                             @RequestParam String lastName,
                             @RequestParam String street,
                             @RequestParam String city,
                             @RequestParam String state,
                             @RequestParam String zipCode,
                             @RequestParam(required = false) String buildingNo,
                             @RequestParam(required = false) boolean isRenter,
                             @RequestParam(required = false) boolean isProvider,
                             HttpSession session) {
        
        try {
            User user = User.builder()
                    .username(username)
                    .password(password)
                    .firstName(firstName)
                    .lastName(lastName)
                    .address(com.example.termproject.model.Address.builder()
                            .street(street)
                            .city(city)
                            .state(state)
                            .zipCode(zipCode)
                            .buildingNo(buildingNo)
                            .build())
                    .build();

            User registered = userService.registerUser(user, isRenter, isProvider);
            
            session.setAttribute("user", registered.getUsername());
            session.setAttribute("userId", registered.getUserId());
            session.setAttribute("roles", userRepository.findRolesByUserId(registered.getUserId()));
            
            return "redirect:/dashboard";
        } catch (Exception e) {
            return "redirect:/register?error=" + e.getMessage();
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username, @RequestParam String password, HttpSession session) {
        User user = userService.findByUsername(username);
        if (user == null || !userService.verifyPassword(user, password)) {
            return "redirect:/login?error=Invalid username or password.";
        }
        
        session.setAttribute("user", user.getUsername());
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("roles", userRepository.findRolesByUserId(user.getUserId()));
        return "redirect:/dashboard";
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String view, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        List<String> roles = (List<String>) session.getAttribute("roles");
        if (roles == null) {
            roles = userRepository.findRolesByUserId(userId);
            session.setAttribute("roles", roles);
        }
        
        model.addAttribute("roles", roles);
        model.addAttribute("username", session.getAttribute("user"));
        model.addAttribute("currentUserId", userId);

        String activeView = view;
        if (roles.size() == 1) {
            activeView = roles.get(0).toLowerCase();
        } else if (activeView == null || (!activeView.equalsIgnoreCase("renter") && !activeView.equalsIgnoreCase("provider") && !activeView.equalsIgnoreCase("admin"))) {
            activeView = roles.get(0).toLowerCase();
        } else {
            final String finalView = activeView;
            if (roles.stream().noneMatch(r -> r.equalsIgnoreCase(finalView))) {
                activeView = roles.get(0).toLowerCase();
            }
        }
        
        model.addAttribute("activeView", activeView.toLowerCase());

        if ("provider".equalsIgnoreCase(activeView)) {
            model.addAttribute("myEquipment", equipmentRepository.findByProviderId(userId));
            model.addAttribute("incomingBookings", bookingService.getIncomingAppointments(userId));
        } else if ("admin".equalsIgnoreCase(activeView)) {
            model.addAttribute("reportedEquipment", equipmentRepository.findReported());
            model.addAttribute("allEquipment", equipmentRepository.findAll());
        } else {
            List<com.example.termproject.model.Equipment> allEquipment = equipmentRepository.findAll();
            List<com.example.termproject.model.Equipment> availableEquipment = allEquipment.stream()
                    .filter(e -> !e.getProviderId().equals(userId) && !e.isDeactivated())
                    .toList();
            
            model.addAttribute("equipmentList", availableEquipment);
            model.addAttribute("myBookings", bookingService.getRenterHistory(userId));
        }

        // Combined Chat Partners: Message History + Booking Relationships
        Set<Long> partnerIds = new HashSet<>(messageRepository.findInteractedUserIds(userId));
        partnerIds.addAll(bookingRepository.findPartnersByUserId(userId));
        
        List<Map<String, Object>> chatUsers = new ArrayList<>();
        for (Long otherId : partnerIds) {
            userRepository.findById(otherId).ifPresent(otherUser -> {
                Map<String, Object> uMap = new HashMap<>();
                uMap.put("userId", otherUser.getUserId());
                uMap.put("username", otherUser.getUsername());
                chatUsers.add(uMap);
            });
        }
        model.addAttribute("chatUsers", chatUsers);
        
        return "dashboard";
    }

    @PostMapping("/equipment/add")
    public String addEquipment(@RequestParam String name, 
                               @RequestParam String description, 
                               @RequestParam java.math.BigDecimal hourlyRate, 
                               HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        com.example.termproject.model.Equipment equipment = com.example.termproject.model.Equipment.builder()
                .providerId(userId)
                .name(name)
                .description(description)
                .hourlyRate(hourlyRate)
                .build();
        
        equipmentRepository.save(equipment);
        return "redirect:/dashboard?view=provider";
    }

    @PostMapping("/book")
    public String book(@RequestParam Long equipmentId, 
                       @RequestParam String startTime, 
                       @RequestParam int durationHours, 
                       HttpSession session) {
        
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login";

        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime start = LocalDateTime.parse(startTime, formatter);
            LocalDateTime end = start.plusHours(durationHours);

            Booking booking = Booking.builder()
                    .renterId(userId)
                    .equipmentId(equipmentId)
                    .startTime(start)
                    .endTime(end)
                    .build();

            bookingService.bookEquipment(booking);
            return "redirect:/dashboard?success=booked";
        } catch (Exception e) {
            return "redirect:/dashboard?error=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/equipment/report")
    public String reportEquipment(@RequestParam Long equipmentId) {
        equipmentRepository.report(equipmentId);
        return "redirect:/dashboard?view=renter&success=reported";
    }

    @PostMapping("/equipment/deactivate")
    public String deactivateEquipment(@RequestParam Long equipmentId, @RequestParam boolean deactivated, @RequestParam(required = false) String redirectView) {
        equipmentRepository.deactivate(equipmentId, deactivated);
        String view = (redirectView != null) ? redirectView : "provider";
        return "redirect:/dashboard?view=" + view + "&success=updated";
    }

    @PostMapping("/equipment/delete")
    public String deleteEquipment(@RequestParam Long equipmentId, @RequestParam(required = false) String redirectView) {
        equipmentRepository.delete(equipmentId);
        String view = (redirectView != null) ? redirectView : "provider";
        return "redirect:/dashboard?view=" + view + "&success=deleted";
    }

    // CHAT API
    @GetMapping("/api/chat/history")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@RequestParam Long otherUserId, @RequestParam Long equipmentId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return new ArrayList<>();
        return messageRepository.findChatHistory(userId, otherUserId, equipmentId);
    }

    @PostMapping("/api/chat/send")
    @ResponseBody
    public ChatMessage sendMessage(@RequestParam Long receiverId, @RequestParam Long equipmentId, @RequestParam String content, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return null;

        ChatMessage msg = ChatMessage.builder()
                .senderId(userId)
                .receiverId(receiverId)
                .equipmentId(equipmentId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
        
        return messageRepository.save(msg);
    }

    @GetMapping("/api/chat/equipment")
    @ResponseBody
    public List<com.example.termproject.model.Equipment> getSharedEquipment(@RequestParam Long otherUserId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return new ArrayList<>();
        
        List<com.example.termproject.model.Equipment> listA = equipmentRepository.findByProviderId(userId);
        List<com.example.termproject.model.Equipment> listB = equipmentRepository.findByProviderId(otherUserId);
        
        List<com.example.termproject.model.Equipment> combined = new ArrayList<>();
        combined.addAll(listA);
        combined.addAll(listB);
        return combined;
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // Milestone 6: System Management Redirection
    @GetMapping("/health")
    public String health() {
        return "redirect:/actuator/health";
    }

    @GetMapping("/metrics")
    public String metrics() {
        return "redirect:/actuator/metrics";
    }
}
