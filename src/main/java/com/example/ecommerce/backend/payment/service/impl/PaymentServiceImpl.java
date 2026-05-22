package com.example.ecommerce.backend.payment.service.impl;

import com.example.ecommerce.backend.common.exception.ResourceConflictException;
import com.example.ecommerce.backend.inventory.entity.Inventory;
import com.example.ecommerce.backend.inventory.repository.InventoryRepository;
import com.example.ecommerce.backend.mail.service.impl.PaymentConfirmationMail;
import com.example.ecommerce.backend.order.entity.Order;
import com.example.ecommerce.backend.order.entity.OrderItem;
import com.example.ecommerce.backend.order.entity.OrderStatus;
import com.example.ecommerce.backend.order.repository.OrderRepository;
import com.example.ecommerce.backend.payment.config.PaymentExpirationProperties;
import com.example.ecommerce.backend.payment.config.StripeConfig;
import com.example.ecommerce.backend.payment.dto.response.PaymentResponse;
import com.example.ecommerce.backend.payment.entity.PaymentHistory;
import com.example.ecommerce.backend.payment.enums.PaymentStatus;
import com.example.ecommerce.backend.payment.repository.PaymentHistoryRepository;
import com.example.ecommerce.backend.payment.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Stripe-backed implementation of {@link PaymentService}.
 *
 * <p>The service creates hosted Stripe Checkout Sessions after orders are
 * placed and records the session details in payment history. Status handlers
 * are intentionally service-level operations so future webhook, redirect, or
 * scheduler adapters can reuse the same business rules without duplicating
 * payment state transitions.</p>
 *
 * @author Pial Kanti Samadder
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final StripeConfig stripeConfig;
    private final PaymentExpirationProperties paymentExpirationProperties;
    private final PaymentConfirmationMail paymentConfirmation;
    /**
     * Creates a Stripe Checkout Session for a confirmed order and stores the
     * initiated payment attempt.
     *
     * @param orderId order identifier
     * @return payment details containing the Stripe checkout URL
     */
    @Override
    @Transactional
    public PaymentResponse initiatePayment(Long orderId) {
        log.info("Initiating Stripe payment for orderId={}", orderId);

        Order order = findOrder(orderId);
        validatePayableOrder(order);

        Optional<PaymentHistory> latestInitiatedPayment = paymentHistoryRepository
                .findTopByOrderIdAndStatusOrderByCreatedAtDesc(orderId, PaymentStatus.INITIATED);

        if (latestInitiatedPayment.isPresent()) {
            PaymentHistory paymentHistory = latestInitiatedPayment.get();
            if (paymentHistory.getExpiresAt().isAfter(LocalDateTime.now())) {
                return toResponse(paymentHistory);
            }

            cancelStaleInitiatedPayment(paymentHistory);
        }

        return createPayment(order);
    }

    /**
     * Marks a payment attempt as successful and moves the linked order to paid.
     *
     * @param sessionId Stripe checkout session identifier
     */
    @Override
    @Transactional
    public void handleSuccessfulPayment(String sessionId) {
        log.info("Handling successful Stripe payment for sessionId={}", sessionId);

        PaymentHistory paymentHistory = findPaymentHistory(sessionId);
        if (paymentHistory.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Successful payment callback ignored because payment is already successful. sessionId={}", sessionId);
            return;
        }

        if (paymentHistory.getStatus() == PaymentStatus.CANCELLED) {
            log.warn("Successful payment rejected because payment is cancelled. sessionId={}", sessionId);
            throw new ResourceConflictException("Cancelled payment cannot be marked successful.");
        }

        paymentHistory.setStatus(PaymentStatus.SUCCESS);
        paymentHistory.setModifiedBy(paymentHistory.getOrder().getUserId());

        Order order = paymentHistory.getOrder();
        order.getItems().forEach(this::consumeReservedInventory);
        order.setStatus(OrderStatus.PAID);
        order.setModifiedBy(order.getUserId());

        orderRepository.save(order);
        paymentHistoryRepository.save(paymentHistory);

        //email caller
        paymentConfirmation.sendPaymentConfirmation(order,paymentHistory);


        log.info("Successful payment handled for sessionId={}, orderId={}", sessionId, order.getId());
    }

    /**
     * Marks a payment attempt as failed while keeping the linked order confirmed
     * for future timeout cancellation.
     *
     * @param sessionId Stripe checkout session identifier
     */
    @Override
    @Transactional
    public void handleFailedPayment(String sessionId) {
        log.info("Handling failed Stripe payment for sessionId={}", sessionId);

        PaymentHistory paymentHistory = findPaymentHistory(sessionId);
        if (paymentHistory.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Failed payment callback ignored because payment is already successful. sessionId={}", sessionId);
            return;
        }

        if (paymentHistory.getStatus() == PaymentStatus.FAILED) {
            log.info("Failed payment callback ignored because payment is already failed. sessionId={}", sessionId);
            return;
        }

        if (paymentHistory.getStatus() == PaymentStatus.CANCELLED) {
            log.info("Failed payment callback ignored because payment is already cancelled. sessionId={}", sessionId);
            return;
        }

        paymentHistory.setStatus(PaymentStatus.FAILED);
        paymentHistory.setModifiedBy(paymentHistory.getOrder().getUserId());
        paymentHistoryRepository.save(paymentHistory);
        log.info("Failed payment handled for sessionId={}, orderId={}", sessionId, paymentHistory.getOrder().getId());
    }

    private PaymentResponse createPayment(Order order) {
        try {
            Session session = Session.create(buildSessionCreateParams(order));

            PaymentHistory paymentHistory = PaymentHistory.builder()
                    .order(order)
                    .sessionId(session.getId())
                    .paymentLink(session.getUrl())
                    .status(PaymentStatus.INITIATED)
                    .expiresAt(LocalDateTime.now().plus(paymentExpirationProperties.getLifetime()))
                    .createdBy(order.getUserId())
                    .modifiedBy(order.getUserId())
                    .build();

            PaymentHistory savedPaymentHistory = paymentHistoryRepository.saveAndFlush(paymentHistory);
            log.info("Stripe payment initiated for orderId={}, sessionId={}", order.getId(), session.getId());
            return toResponse(savedPaymentHistory);
        } catch (StripeException exception) {
            log.error("Stripe payment initiation failed for orderId={}", order.getId(), exception);
            throw new ResourceConflictException("Unable to initiate payment. Please try again.");
        }
    }

    private void cancelStaleInitiatedPayment(PaymentHistory paymentHistory) {
        expireStripeSession(paymentHistory.getSessionId());
        paymentHistory.setStatus(PaymentStatus.CANCELLED);
        paymentHistory.setModifiedBy(paymentHistory.getOrder().getUserId());
        paymentHistoryRepository.save(paymentHistory);
        log.info("Expired initiated payment cancelled before creating a new attempt. paymentHistoryId={}, orderId={}",
                paymentHistory.getId(), paymentHistory.getOrder().getId());
    }

    private void expireStripeSession(String sessionId) {
        try {
            Session.retrieve(sessionId).expire();
        } catch (StripeException exception) {
            log.warn("Unable to expire stale Stripe checkout session. sessionId={}", sessionId, exception);
        }
    }

    private SessionCreateParams buildSessionCreateParams(Order order) {
        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeConfig.getSuccessUrl())
                .setCancelUrl(stripeConfig.getCancelUrl())
                .setClientReferenceId(order.getId().toString())
                .putMetadata("orderId", order.getId().toString())
                .putMetadata("orderNumber", order.getOrderNumber().toString())
                .putMetadata("userId", order.getUserId().toString());

        order.getItems().forEach(item -> builder.addLineItem(toStripeLineItem(item)));
        return builder.build();
    }

    private SessionCreateParams.LineItem toStripeLineItem(OrderItem item) {
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(item.getProductName())
                        .putMetadata("productId", item.getProductId().toString())
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(stripeConfig.getCurrency())
                        .setUnitAmount(toMinorUnitAmount(item.getUnitPrice()))
                        .setProductData(productData)
                        .build();

        return SessionCreateParams.LineItem.builder()
                .setQuantity(item.getQuantity().longValue())
                .setPriceData(priceData)
                .build();
    }

    private Long toMinorUnitAmount(Double amount) {
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
    }

    private PaymentHistory findPaymentHistory(String sessionId) {
        return paymentHistoryRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for session: " + sessionId));
    }

    private void consumeReservedInventory(OrderItem item) {
        Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product: " + item.getProductId()));

        if (item.getQuantity() > inventory.getReservedQuantity()) {
            log.warn("Cannot consume reserved inventory for productId={}, requested={}, reserved={}",
                    item.getProductId(), item.getQuantity(), inventory.getReservedQuantity());
            throw new ResourceConflictException("Cannot consume more inventory than currently reserved.");
        }

        inventory.setTotalQuantity(inventory.getTotalQuantity() - item.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() - item.getQuantity());
        inventoryRepository.saveAndFlush(inventory);
    }

    private void validatePayableOrder(Order order) {
        if (order.getStatus() == OrderStatus.PAID) {
            log.warn("Payment initiation rejected because order is already paid. orderId={}", order.getId());
            throw new ResourceConflictException("Order is already paid.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("Payment initiation rejected because order is cancelled. orderId={}", order.getId());
            throw new ResourceConflictException("Cancelled order cannot be paid.");
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            log.warn("Payment initiation rejected because order status is not payable. orderId={}, status={}",
                    order.getId(), order.getStatus());
            throw new ResourceConflictException("Only confirmed orders can be paid.");
        }
    }

    private PaymentResponse toResponse(PaymentHistory paymentHistory) {
        return new PaymentResponse(
                paymentHistory.getId(),
                paymentHistory.getOrder().getId(),
                paymentHistory.getSessionId(),
                paymentHistory.getPaymentLink(),
                paymentHistory.getStatus(),
                paymentHistory.getExpiresAt(),
                paymentHistory.getCreatedAt(),
                paymentHistory.getModifiedAt()
        );
    }
}
