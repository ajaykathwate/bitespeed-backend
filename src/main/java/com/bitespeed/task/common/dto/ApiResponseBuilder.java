package com.bitespeed.task.common.dto;

import com.bitespeed.task.contact.dto.ContactResponseDto;
import com.bitespeed.task.contact.entity.Contact;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ApiResponseBuilder {

    public ContactResponseDto buildResponse(Contact primary, List<Contact> contacts) {
        List<String> emails = contacts.stream()
                .map(Contact::getEmail)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<String> phoneNumbers = contacts.stream()
                .map(Contact::getPhoneNumber)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Integer> secondaryIds = contacts.stream()
                .filter(c -> !c.getId().equals(primary.getId()))
                .map(Contact::getId)
                .toList();

        return ContactResponseDto.builder()
                .primaryContactId(primary.getId().intValue())
                .emails(emails)
                .phoneNumbers(phoneNumbers)
                .secondaryContactIds(secondaryIds)
                .build();
    }

}
