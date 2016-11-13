package io.blep;

import org.junit.Test;

import javax.persistence.*;

/**
 * @author blep
 */
public class LobConverterTest {

    @Test
    public void testName() throws Exception {
        final EntityManagerFactory emf = Persistence.createEntityManagerFactory("samplePU");

        final EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();
        em.persist(new MyEntity());
        em.getTransaction().commit();
    }
}
