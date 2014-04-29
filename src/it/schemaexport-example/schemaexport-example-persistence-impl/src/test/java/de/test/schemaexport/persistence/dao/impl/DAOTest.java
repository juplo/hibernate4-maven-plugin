package de.test.schemaexport.persistence.dao.impl;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.test.schemaexport.domain.Department;

public class DAOTest {
	
	private static EntityManagerFactory emf;
	private EntityManager em;
	private DepartmentDAO departmentDAO = new DepartmentDAO();
	
	@BeforeClass
	public static void setUpClass() {
		// Use persistence.xml configuration
		emf = Persistence.createEntityManagerFactory("swmtestappManagerTest");
		Assert.assertNotNull(emf);
	}
	
	@Before
	public void setUp() {
		em = emf.createEntityManager(); // Retrieve an application managed entity manager
		Assert.assertNotNull(em);
		
		EntityTransaction tx = em.getTransaction();
		Assert.assertNotNull(tx);
		
		tx.begin();
		tx.setRollbackOnly();
	}
	
	@Test
	public void testSomething() {
		Department department = new Department();
		department.setName("Dep");
		Department result = departmentDAO.createOrUpdate(em, department);
		System.out.println(result.getOid());
	}
	
	@After
	public void tearDown() {
		em.getTransaction().rollback();
		//em.getTransaction().commit();
		em.close();
	}
	
	@AfterClass
	public static void tearDownClass() {

		emf.close();
	}
	
	
}
