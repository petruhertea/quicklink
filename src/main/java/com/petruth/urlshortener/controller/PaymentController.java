package com.petruth.urlshortener.controller;

import com.petruth.urlshortener.entity.User;
import com.petruth.urlshortener.service.PaymentService;
import com.petruth.urlshortener.service.UserService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    public PaymentController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @PostMapping("/create-checkout-session")
    public String createCheckoutSession(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            String email = principal.getAttribute("email");
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.isPremium()) {
                return "redirect:/dashboard?already-premium=true";
            }

            String checkoutUrl = paymentService.createCheckoutSession(user);
            return "redirect:" + checkoutUrl;

        } catch (StripeException e) {
            e.printStackTrace();
            return "redirect:/dashboard?payment-error=true";
        }
    }

    @GetMapping("/success")
    public String paymentSuccess(@RequestParam("session_id") String sessionId, Model model) {
        model.addAttribute("sessionId", sessionId);
        return "payment-success";
    }

    @GetMapping("/cancel")
    public String paymentCancel() {
        return "redirect:/dashboard?payment-cancelled=true";
    }

    @PostMapping("/webhook")
    @ResponseBody
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        // Handle the event
        switch (event.getType()) {
            case "checkout.session.completed":
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject()
                        .orElse(null);

                if (session != null) {
                    handleSuccessfulPayment(session);
                }
                break;

            case "customer.subscription.deleted":
                // Handle subscription cancellation
                Session cancelSession = (Session) event.getDataObjectDeserializer()
                        .getObject()
                        .orElse(null);

                if (cancelSession != null) {
                    handleSubscriptionCancelled(cancelSession);
                }
                break;

            default:
                System.out.println("Unhandled event type: " + event.getType());
        }

        return ResponseEntity.ok("Success");
    }

    private void handleSuccessfulPayment(Session session) {
        String userId = session.getMetadata().get("userId");
        if (userId == null) {
            System.out.println("No userId in session metadata");
            return;
        }

        userService.findById(Long.parseLong(userId)).ifPresent(user -> {
            user.setPremium(true);
            userService.save(user);
            System.out.println("User " + user.getEmail() + " upgraded to premium");
        });
    }


    private void handleSubscriptionCancelled(Session session) {
        String userId = session.getClientReferenceId();
        if (userId != null) {
            User user = userService.findById(Long.parseLong(userId))
                    .orElse(null);

            if (user != null) {
                user.setPremium(false);
                userService.save(user);
                System.out.println("User " + user.getEmail() + " downgraded from premium");
            }
        }
    }
}