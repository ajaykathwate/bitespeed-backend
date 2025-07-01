package com.bitespeed.task.contact.api;

import com.bitespeed.task.common.dto.ApiResponseBuilder;
import com.bitespeed.task.contact.dto.ContactRequestDto;
import com.bitespeed.task.contact.dto.ContactResponseDto;
import com.bitespeed.task.contact.entity.Contact;
import com.bitespeed.task.contact.entity.LinkPrecedence;
import com.bitespeed.task.contact.repository.ContactRepository;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentifyContactService {

    private final ContactRepository contactRepository;
    private final ApiResponseBuilder apiResponseBuilder;

    public ContactResponseDto identifyContact(ContactRequestDto request) {
        String email = request.getEmail();
        String phone = request.getPhoneNumber();

        List<Contact> initialMatches = contactRepository.findAllByEmailOrPhoneNumberAndDeletedAtIsNull(email, phone);

        if (initialMatches.isEmpty()) {
            Contact newPrimary = Contact.builder()
                    .email(email)
                    .phoneNumber(phone)
                    .linkPrecedence(LinkPrecedence.PRIMARY)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            contactRepository.save(newPrimary);
            return apiResponseBuilder.buildResponse(newPrimary, List.of(newPrimary));
        }

        Set<Integer> relatedIds = new HashSet<>();
        for (Contact contact : initialMatches) {
            relatedIds.add(contact.getId());
            if (contact.getLinkedId() != null) {
                relatedIds.add(contact.getLinkedId());
            }
        }

        Set<Integer> visitedIds = new HashSet<>();
        Queue<Integer> queue = new LinkedList<Integer>(relatedIds);
        while (!queue.isEmpty()) {
            Integer currentId = queue.poll();
            if (visitedIds.contains(currentId)) continue;
            visitedIds.add(currentId);

            List<Contact> connected = contactRepository.findAllByLinkedIdOrIdAndDeletedAtIsNull(currentId, currentId);
            for (Contact c : connected) {
                if (!visitedIds.contains(c.getId())) queue.add(c.getId());
                if (c.getLinkedId() != null && !visitedIds.contains(c.getLinkedId())) queue.add(c.getLinkedId());
            }
        }

        List<Contact> fullGroup = contactRepository.findAllById(visitedIds);

        Contact primary = fullGroup.stream()
                .filter(c -> c.getLinkPrecedence() == LinkPrecedence.PRIMARY)
                .min(Comparator.comparing(Contact::getCreatedAt))
                .orElseThrow();

        for (Contact contact : fullGroup) {
            if (!contact.getId().equals(primary.getId()) && contact.getLinkPrecedence() == LinkPrecedence.PRIMARY) {
                contact.setLinkPrecedence(LinkPrecedence.SECONDARY);
                contact.setLinkedId(primary.getId());
                contact.setUpdatedAt(LocalDateTime.now());
                contactRepository.save(contact);
            }
        }

        boolean emailExists = fullGroup.stream().anyMatch(c -> Objects.equals(c.getEmail(), email));
        boolean phoneExists = fullGroup.stream().anyMatch(c -> Objects.equals(c.getPhoneNumber(), phone));

        if ((!emailExists && email != null) || (!phoneExists && phone != null)) {
            Contact newSecondary = Contact.builder()
                    .email(email)
                    .phoneNumber(phone)
                    .linkedId(primary.getId())
                    .linkPrecedence(LinkPrecedence.SECONDARY)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            contactRepository.save(newSecondary);
            fullGroup.add(newSecondary);
        }

        return apiResponseBuilder.buildResponse(primary, fullGroup);
    }
}
