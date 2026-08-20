package com.dms.rescueService.rescue.exception;

public class DepartmentNotFoundException extends RuntimeException{
    public DepartmentNotFoundException(String error){
        super(error);
    }
}
