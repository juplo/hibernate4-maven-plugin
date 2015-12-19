package a;

import javax.persistence.Column;
import javax.persistence.Id;

public class Employee {    
    private int empId;
    private String empName;
    private double empSalary;
    public Employee() {
    }
    public Employee(int empId, String empName, double empSalary) {
        this.empId = empId;
        this.empName = empName;
        this.empSalary = empSalary;
    }
    @Id
    @Column(name = "WRONG_NAME")
    public int getEmpId() {
        return empId;
    }
    public void setEmpId(int empId) {
        this.empId = empId;
    }
    public String getEmpName() {
        return empName;
    }
    public void setEmpName(String empName) {
        this.empName = empName;
    }
    public double getEmpSalary() {
        return empSalary;
    }
    public void setEmpSalary(double empSalary) {
        this.empSalary = empSalary;
    }
    @Override
    public String toString() {
        return "Employee Id:="+empId+" Employee Name:="+empName+" Employee Salary:="+empSalary;
    }
}//End of Employee.java
