package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.tbloginuser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface tbloginuserRepository extends JpaRepository<tbloginuser, Integer> {
    int countByDepId(Integer DepID);
}
