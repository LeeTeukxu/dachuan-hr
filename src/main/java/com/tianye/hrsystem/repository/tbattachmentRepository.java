package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.tbattachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface tbattachmentRepository extends JpaRepository<tbattachment, String> {

    List<tbattachment> findAllByTypeOrderByCreateTimeDesc(String Type);
}
