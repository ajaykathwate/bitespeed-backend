package com.bitespeed.task.contact;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bitespeed.task.contact.dto.ContactRequestDto;
import com.bitespeed.task.contact.entity.Contact;
import com.bitespeed.task.contact.entity.LinkPrecedence;
import com.bitespeed.task.contact.repository.ContactRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ContactEndpointTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ContactRepository contactRepository;

    @BeforeEach
    void setup() {
        contactRepository.deleteAll();
    }

    @Test
    void noExistingContact_createsNewPrimary() throws Exception {
        ContactRequestDto request = new ContactRequestDto("ajay@example.com", "123456");

        mockMvc.perform(post("/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.primaryContactId").exists())
                .andExpect(jsonPath("$.contact.emails[0]").value("ajay@example.com"))
                .andExpect(jsonPath("$.contact.phoneNumbers[0]").value("123456"))
                .andExpect(jsonPath("$.contact.secondaryContactIds").isEmpty());
    }

    @Test
    void existingContactByPhone_addsSecondary() throws Exception {
        Contact primary = contactRepository.save(Contact.builder()
                .email("lorraine@hillvalley.edu")
                .phoneNumber("123456")
                .linkPrecedence(LinkPrecedence.PRIMARY)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build());

        ContactRequestDto request = new ContactRequestDto("mcfly@hillvalley.edu", "123456");

        mockMvc.perform(post("/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.primaryContactId").value(primary.getId()))
                .andExpect(jsonPath("$.contact.emails", hasItems("lorraine@hillvalley.edu", "mcfly@hillvalley.edu")))
                .andExpect(jsonPath("$.contact.phoneNumbers", hasItem("123456")))
                .andExpect(jsonPath("$.contact.secondaryContactIds").isNotEmpty());
    }

    @Test
    void existingContactByEmail_addsSecondary() throws Exception {
        Contact primary = contactRepository.save(Contact.builder()
                .email("mcfly@hillvalley.edu")
                .phoneNumber("999999")
                .linkPrecedence(LinkPrecedence.PRIMARY)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build());

        ContactRequestDto request = new ContactRequestDto("mcfly@hillvalley.edu", "123456");

        mockMvc.perform(post("/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.primaryContactId").value(primary.getId()))
                .andExpect(jsonPath("$.contact.phoneNumbers", hasItems("999999", "123456")))
                .andExpect(jsonPath("$.contact.secondaryContactIds").isNotEmpty());
    }

    @Test
    void phoneAndEmailMatchSameContact_doesNotCreateNew() throws Exception {
        Contact existing = contactRepository.save(Contact.builder()
                .email("ajay@example.com")
                .phoneNumber("123456")
                .linkPrecedence(LinkPrecedence.PRIMARY)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        ContactRequestDto request = new ContactRequestDto("ajay@example.com", "123456");

        mockMvc.perform(post("/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.primaryContactId").value(existing.getId()))
                .andExpect(jsonPath("$.contact.secondaryContactIds").isEmpty());
    }

    @Test
    void phoneAndEmailMatchDifferentContacts_mergesThem() throws Exception {
        Contact older = contactRepository.save(Contact.builder()
                .email("george@hillvalley.edu")
                .phoneNumber("919191")
                .linkPrecedence(LinkPrecedence.PRIMARY)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(10))
                .build());

        Contact newer = contactRepository.save(Contact.builder()
                .email("biffsucks@hillvalley.edu")
                .phoneNumber("717171")
                .linkPrecedence(LinkPrecedence.PRIMARY)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build());

        ContactRequestDto request = new ContactRequestDto("george@hillvalley.edu", "717171");

        mockMvc.perform(post("/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.primaryContactId").value(older.getId()))
                .andExpect(jsonPath("$.contact.emails", hasItems("george@hillvalley.edu", "biffsucks@hillvalley.edu")))
                .andExpect(jsonPath("$.contact.phoneNumbers", hasItems("919191", "717171")))
                .andExpect(jsonPath("$.contact.secondaryContactIds").value(hasItem(newer.getId().intValue())));
    }


    @Test
    void phoneNull_onlyEmailMatch() throws Exception {
        contactRepository.save(Contact.builder()
                .email("ajay@example.com")
                .phoneNumber("555555")
                .linkPrecedence(LinkPrecedence.PRIMARY)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        ContactRequestDto request = new ContactRequestDto("ajay@example.com", null);

        mockMvc.perform(post("/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.emails", hasItem("ajay@example.com")))
                .andExpect(jsonPath("$.contact.secondaryContactIds").isEmpty());
    }

    @Test
    void emailNull_onlyPhoneMatch() throws Exception {
        contactRepository.save(Contact.builder()
                .email("someone@example.com")
                .phoneNumber("888888")
                .linkPrecedence(LinkPrecedence.PRIMARY)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        ContactRequestDto request = new ContactRequestDto(null, "888888");

        mockMvc.perform(post("/identify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.phoneNumbers", hasItem("888888")))
                .andExpect(jsonPath("$.contact.secondaryContactIds").isEmpty());
    }
}