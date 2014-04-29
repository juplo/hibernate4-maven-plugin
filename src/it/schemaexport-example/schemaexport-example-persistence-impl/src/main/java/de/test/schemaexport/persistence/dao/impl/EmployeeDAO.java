package de.test.schemaexport.persistence.dao.impl;

import javax.persistence.EntityManager;

import de.test.schemaexport.domain.Employee;
import de.test.schemaexport.persistence.dao.IEmployeeDAO;

public class EmployeeDAO implements IEmployeeDAO {

	public Employee findByID(EntityManager em, long id) {
		return em.find(Employee.class, id);
	}

	public Employee createOrUpdate(EntityManager em, Employee toCreateOrUpdate) {
		return em.merge(toCreateOrUpdate);
	}

	
}
