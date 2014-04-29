package de.test.schemaexport.persistence.dao.impl;

import javax.persistence.EntityManager;
import de.test.schemaexport.domain.Department;
import de.test.schemaexport.persistence.dao.IDepartmentDAO;

public class DepartmentDAO implements IDepartmentDAO {

	public Department findByID(EntityManager em, long id) {
		return em.find(Department.class, id);
	}

	public Department createOrUpdate(EntityManager em, Department toCreateOrUpdate) {
		return em.merge(toCreateOrUpdate);
	}

}
