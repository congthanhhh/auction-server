package com.thanh.auction_server.service.auction;

import com.thanh.auction_server.constants.ErrorMessage;
import com.thanh.auction_server.constants.FeedbackRating;
import com.thanh.auction_server.constants.InvoiceStatus;
import com.thanh.auction_server.dto.request.FeedbackRequest;
import com.thanh.auction_server.dto.response.MessageResponse;
import com.thanh.auction_server.entity.Feedback;
import com.thanh.auction_server.entity.Invoice;
import com.thanh.auction_server.entity.User;
import com.thanh.auction_server.exception.DataConflictException;
import com.thanh.auction_server.exception.ResourceNotFoundException;
import com.thanh.auction_server.exception.UnauthorizedException;
import com.thanh.auction_server.exception.UserNotFoundException;
import com.thanh.auction_server.repository.FeedbackRepository;
import com.thanh.auction_server.repository.InvoiceRepository;
import com.thanh.auction_server.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Service
public class FeedbackService {
    FeedbackRepository feedbackRepository;
    InvoiceRepository invoiceRepository;
    UserRepository userRepository;

    @Transactional
    public MessageResponse createFeedback(Long invoiceId, FeedbackRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND + currentUsername));

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVOICE_NOT_FOUND + invoiceId));

        boolean isBuyerEvaluating = currentUser.getId().equals(invoice.getUser().getId());
        boolean isSellerEvaluating = currentUser.getId().equals(invoice.getProduct().getSeller().getId());

        if (!isBuyerEvaluating && !isSellerEvaluating) {
            throw new UnauthorizedException(ErrorMessage.UNAUTHORIZED_ACCESS);
        }
        if (invoice.getStatus() == InvoiceStatus.COMPLETED) {
            if (isSellerEvaluating && request.getRating() == FeedbackRating.NEGATIVE) {
                throw new DataConflictException(ErrorMessage.CANNOT_GIVE_FEEDBACK);
            }
        }
        else if (invoice.getStatus() == InvoiceStatus.CANCELLED_NON_PAYMENT) {
            if (isBuyerEvaluating) {
                throw new DataConflictException(ErrorMessage.CANNOT_GIVE_FEEDBACK);
            }
        }
        else {
            throw new DataConflictException(ErrorMessage.CANNOT_GIVE_FEEDBACK + ErrorMessage.STATUS_INCORRECT);
        }
        if (feedbackRepository.existsByInvoice_IdAndFromUser_Id(invoiceId, currentUser.getId())) {
            throw new DataConflictException(ErrorMessage.FEEDBACK_ALREADY_EXISTS);
        }

        // Xác định người nhận (Target User)
        User toUser = isBuyerEvaluating ? invoice.getProduct().getSeller() : invoice.getUser();
        Feedback feedback = Feedback.builder()
                .fromUser(currentUser)
                .toUser(toUser)
                .invoice(invoice)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();
        feedbackRepository.save(feedback);
        int scoreChange = request.getRating().getValue();
        int currentScore = toUser.getReputationScore() == null ? 0 : toUser.getReputationScore();
        toUser.setReputationScore(currentScore + scoreChange);
        userRepository.save(toUser);
        return MessageResponse.builder()
                .message("Update reputation" + (scoreChange > 0 ? "+" : "") + scoreChange)
                .build();
    }

    @Transactional
    public String updateFeedback(Long feedbackId, FeedbackRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.FEEDBACK_NOT_FOUND + feedbackId));
        if (!feedback.getFromUser().getUsername().equals(currentUsername)) {
            throw new UnauthorizedException(ErrorMessage.CANNOT_GIVE_FEEDBACK);
        }
        User targetUser = feedback.getToUser();
        int oldRatingValue = feedback.getRating().getValue();
        int newRatingValue = request.getRating().getValue();
        if (oldRatingValue != newRatingValue) {
            int currentScore = targetUser.getReputationScore() == null ? 0 : targetUser.getReputationScore();
            int newScore = currentScore - oldRatingValue + newRatingValue;
            targetUser.setReputationScore(newScore);
            userRepository.save(targetUser);
        }
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackRepository.save(feedback);

        return "Feedback updated successfully.";

    }

    public Long countFeedbacksForUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND + username));
        return feedbackRepository.countByToUser_Id(currentUser.getId());
    }
}
