package com.translationagency.modules.order.application;

import com.translationagency.modules.order.domain.OrderNote;
import com.translationagency.modules.order.domain.TranslationOrder;
import com.translationagency.modules.order.infrastructure.OrderNoteRepository;
import com.translationagency.modules.order.infrastructure.TranslationOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderNoteService {

    private final OrderNoteRepository orderNoteRepository;
    private final TranslationOrderRepository orderRepository;

    public OrderNoteService(OrderNoteRepository orderNoteRepository,
                            TranslationOrderRepository orderRepository) {
        this.orderNoteRepository = orderNoteRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderNote> getNotesForOrder(UUID orderId) {
        return orderNoteRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    public OrderNote addNote(UUID orderId, String body, String username, String authorName) {
        TranslationOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        OrderNote note = new OrderNote();
        note.setOrder(order);
        note.setBody(body);
        note.setAuthorUsername(username);
        note.setAuthorName(authorName);
        return orderNoteRepository.save(note);
    }
}
