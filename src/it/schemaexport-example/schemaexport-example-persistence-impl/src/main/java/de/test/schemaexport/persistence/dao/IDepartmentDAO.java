package de.test.schemaexport.persistence.dao;

import javax.persistence.EntityManager;
import de.test.schemaexport.domain.Department;

public interface IDepartmentDAO {

	Department findByID(EntityManager em, long id);

	Department createOrUpdate(EntityManager em, Department toCreate);

}
