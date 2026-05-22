package com.example.ecommerce.backend.mail.service.impl;

import com.example.ecommerce.backend.auth.entity.User;
import com.example.ecommerce.backend.auth.repository.UserRepository;
import com.example.ecommerce.backend.order.entity.Order;
import com.example.ecommerce.backend.payment.entity.PaymentHistory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PaymentConfirmationMail {

    private final JavaMailSender mailSender;
    private final UserRepository userRepo;
    User user;

    public PaymentConfirmationMail(JavaMailSender mailSender, UserRepository userRepo) {
        this.mailSender = mailSender;
        this.userRepo = userRepo;
    }

    public void sendPaymentConfirmation(Order order, PaymentHistory paymentHistory) {
        user=userRepo.getReferenceById(order.getUserId()); //double check later
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Payment received for order " + order.getOrderNumber());
        message.setText("Payment Successful");
        mailSender.send(message);
    }
}
