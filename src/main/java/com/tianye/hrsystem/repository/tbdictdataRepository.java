package com.tianye.hrsystem.repository;

import com.tianye.hrsystem.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface tbdictdataRepository  extends JpaRepository<tbdictdata,Integer>  {
    List<tbdictdata> findAllByPid(Integer PID);
    List<tbdictdata> findAllByDtid(Integer DtID);
    List<tbdictdata> findAllByDtidAndPid(Integer DtID,Integer PID);
}
