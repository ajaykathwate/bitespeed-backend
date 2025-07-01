package com.bitespeed.task.contact.endpoint;

import com.bitespeed.task.common.dto.ApiResponse;
import com.bitespeed.task.contact.api.IdentifyContactService;
import com.bitespeed.task.contact.dto.ContactRequestDto;
import com.bitespeed.task.contact.dto.ContactResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/identify")
@RequiredArgsConstructor
@Tag(name = "Contact Identification API", description = "Resolve and link user contacts")
public class ContactEndpoint {

    private final IdentifyContactService identifyContactService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContactResponseDto>> identifyContact(@RequestBody final ContactRequestDto contactRequestDto) {

        ContactResponseDto contact = identifyContactService.identifyContact(contactRequestDto);

        ApiResponse<ContactResponseDto> apiResponse = new ApiResponse<ContactResponseDto>(contact);

        log.info("Contact identified successfully\n{}", apiResponse);

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

}
