package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.tbrolemenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface rolemenuRepository extends JpaRepository<tbrolemenu,Long>  {

    List<tbrolemenu> getAllByRoleId(Integer RoleID);
}
