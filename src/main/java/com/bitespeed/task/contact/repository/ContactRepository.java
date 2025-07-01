package com.bitespeed.task.contact.repository;

import com.bitespeed.task.contact.entity.Contact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Integer> {

    List<Contact> findAllByLinkedIdOrIdAndDeletedAtIsNull(Integer linkedId, Integer id);

    List<Contact> findAllByEmailOrPhoneNumberAndDeletedAtIsNull(String email, String phone);
}
