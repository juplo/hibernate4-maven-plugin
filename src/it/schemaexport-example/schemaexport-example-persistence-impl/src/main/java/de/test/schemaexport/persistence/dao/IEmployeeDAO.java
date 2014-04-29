package de.test.schemaexport.persistence.dao;

import javax.persistence.EntityManager;

import de.test.schemaexport.domain.Employee;

public interface IEmployeeDAO {

	Employee findByID(EntityManager em, long id);
	
	Employee createOrUpdate(EntityManager em, Employee toCreate);

}
