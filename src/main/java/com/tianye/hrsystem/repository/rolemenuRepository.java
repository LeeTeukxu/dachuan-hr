package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.tbrolemenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface rolemenuRepository extends JpaRepository<tbrolemenu,Integer>  {

    List<tbrolemenu> getAllByRoleId(Integer RoleID);
}
