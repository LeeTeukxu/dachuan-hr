package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.tbmenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface tbmenuRepository extends JpaRepository<tbmenu,Integer>  {

}
