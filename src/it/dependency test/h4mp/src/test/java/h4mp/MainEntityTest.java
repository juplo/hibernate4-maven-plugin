package h4mp;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dependent.DependentEntity;

public class MainEntityTest {

	private EntityManager em;
	private EntityTransaction transaction;

	@Before
	public void initializeDependencies() {
		em = Persistence.createEntityManagerFactory("jpaIntegrationTest")
				.createEntityManager();
		transaction = em.getTransaction();
	}

	@SuppressWarnings({ "rawtypes" })
	@Test
	public void test() {
		DependentEntity entity = new DependentEntity();

		transaction.begin();
		em.persist(entity);
		transaction.commit();

		List resultList = em.createNativeQuery(
				"select * from dependententity_aud").getResultList();
		assertThat(resultList.size(), is(1));
	}

	@After
	public void after() {
		em.close();
	}

}
